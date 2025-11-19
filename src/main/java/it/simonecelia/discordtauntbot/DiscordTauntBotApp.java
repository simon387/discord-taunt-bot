package it.simonecelia.discordtauntbot;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import it.simonecelia.discordtauntbot.config.AppConfig;
import it.simonecelia.discordtauntbot.service.business.DiscordTauntBot;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


@Startup
@ApplicationScoped
public final class DiscordTauntBotApp {

	@Inject
	AppConfig appConfig;

	@Inject
	DiscordTauntBot discordTauntBot;

	@PostConstruct
	public void onStartup () throws InterruptedException, IOException {
		Log.info ( "Starting Discord Taunt Bot" );

		if ( appConfig.getDiscordBotToken () == null || appConfig.getDiscordBotToken ().isEmpty () ) {
			Log.error ( "Token not found! Be sure the file secret.properties has been provided" );
			return;
		}
		Log.infof ( "KoTH enabled: %s", appConfig.isKothEnabled () );

		var jdaBuilder = JDABuilder.createDefault ( appConfig.getDiscordBotToken () );
		jdaBuilder.enableIntents ( GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES );
		jdaBuilder.addEventListeners ( discordTauntBot );
		var jda = jdaBuilder.build ();

		jda.awaitReady (); // Blocking until JDA is ready
		setNickName ( jda );
		Log.info ( "Bot connected" );

		appConfig.setJda ( jda );
	}

	private void setNickName ( JDA jda ) throws IOException {
		var versionInfo = "No version found!";
		try ( var in = getClass ().getClassLoader ().getResourceAsStream ( "changelog.txt" ) ) {
			if ( in != null ) {
				try ( var reader = new BufferedReader ( new InputStreamReader ( in ) ) ) {
					versionInfo = reader.readLine ().trim ();
				}
			}
		}
		final var finalVersionInfo = " " + versionInfo.replaceFirst ( "Version ", "v" );
		jda.getGuilds ().forEach ( guild -> guild.modifyNickname ( guild.getSelfMember (), "TauntBot" + finalVersionInfo ).queue (
						success -> Log.infof ( "Nickname changed in guild %s with %s", guild.getName (), finalVersionInfo ),
						error -> Log.warnf ( "Could not change nickname in guild %s: %s", guild.getName (), error.getMessage () )
		) );
	}
}

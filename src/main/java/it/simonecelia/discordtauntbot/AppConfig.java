package it.simonecelia.discordtauntbot;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import net.dv8tion.jda.api.JDA;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.Properties;


@ApplicationScoped
public class AppConfig {

	@ConfigProperty ( name = "admin.id" )
	String adminId;

	@ConfigProperty ( name = "verbose" )
	boolean verbose;

	@ConfigProperty ( name = "channel.id" )
	String channelId;

	@ConfigProperty ( name = "voice.channel.id" )
	String voiceChannelId;

	@ConfigProperty ( name = "guild.id" )
	String guildId;

	@ConfigProperty ( name = "koth.enabled" )
	boolean kothEnabled;

	String discordBotToken;

	JDA jda;

	@PostConstruct
	public void onStartup () {
		var properties = new Properties ();
		try ( var input = AppConfig.class.getClassLoader ().getResourceAsStream ( "secret.properties" ) ) {
			if ( input == null ) {
				Log.error ( "Sorry, unable to find secret.properties" );
				return;
			}
			properties.load ( input );
			discordBotToken = properties.getProperty ( "discord.bot.token" );
		} catch ( IOException ex ) {
			Log.error ( ex );
		}
	}

	public String getAdminId () {
		return adminId;
	}

	public boolean isVerbose () {
		return verbose;
	}

	public void setVerbose ( boolean b ) {
		this.verbose = b;
	}

	public String getChannelId () {
		return channelId;
	}

	public String getVoiceChannelId () {
		return voiceChannelId;
	}

	public String getGuildId () {
		return guildId;
	}

	public boolean isKothEnabled () {
		return kothEnabled;
	}

	public String getDiscordBotToken () {
		return discordBotToken;
	}

	public JDA getJda () {
		return jda;
	}

	public void setJda ( JDA jda ) {
		this.jda = jda;
	}
}

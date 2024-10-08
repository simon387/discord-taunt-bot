package it.simonecelia.discordtauntbot;

import it.simonecelia.discordtauntbot.business.DiscordTauntBot;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public final class DiscordTauntBotApp {

	private static final Logger log = LoggerFactory.getLogger ( DiscordTauntBotApp.class );

	private static final String RESOURCES_DIR = "src/main/resources/";

	public static void main ( String[] args ) {
		log.info ( "Starting Discord Taunt Bot" );

		var properties = new Properties ();
		try {
			properties.load ( new FileInputStream ( RESOURCES_DIR + "secret.properties" ) );
			var token = properties.getProperty ( "discord.bot.token" );

			if ( token == null || token.isEmpty () ) {
				log.error ( "Token not found! Be sure the file secret.properties has been provided" );
				return;
			}
			properties.load ( new FileInputStream ( RESOURCES_DIR + "application.properties" ) );
			var adminID = properties.getProperty ( "admin.id" );
			var verbose = Boolean.parseBoolean ( properties.getProperty ( "verbose" ) );

			var builder = JDABuilder.createDefault ( token );
			builder.enableIntents ( GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES );
			builder.addEventListeners ( new DiscordTauntBot ( adminID, verbose ) );
			builder.build ();
		} catch ( IOException e ) {
			log.error ( e.getMessage (), e );
		}
	}
}

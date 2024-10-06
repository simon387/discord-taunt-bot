package it.simonecelia.discordtauntbot;

import it.simonecelia.discordtauntbot.audio.AudioPlayer;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class DiscordTauntBot extends ListenerAdapter {

	private static final Logger log = LoggerFactory.getLogger ( DiscordTauntBot.class );

	private final AudioPlayer audioPlayer;

	private final String ASSETS_DIR;

	public DiscordTauntBot () {
		audioPlayer = new AudioPlayer ();
		//
		var currentPath = new File ( "" ).getAbsolutePath ();
		log.info ( "App working dir is: {}", currentPath );
		ASSETS_DIR = currentPath + "\\assets\\";
	}

	public static void main ( String[] args ) {
		log.info ( "Starting Discord Taunt Bot" );

		var properties = new Properties ();
		try {
			properties.load ( new FileInputStream ( "src/main/resources/secret.properties" ) );
			var token = properties.getProperty ( "discord.bot.token" );

			if ( token == null || token.isEmpty () ) {
				log.error ( "Token not found!" );
				return;
			}

			var builder = JDABuilder.createDefault ( token );
			builder.enableIntents ( GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES );
			builder.addEventListeners ( new DiscordTauntBot () );
			builder.build ();
		} catch ( IOException e ) {
			log.error ( e.getMessage (), e );
		}
	}

	@Override
	public void onMessageReceived ( MessageReceivedEvent event ) {
		if ( event.getAuthor ().isBot () ) {
			return;
		}

		var message = event.getMessage ();
		var content = message.getContentRaw ();

		log.info ( "Got message from: {}", event.getAuthor () );
		log.info ( "Content: {}", content );

		if ( content.equals ( "/ping" ) ) {
			log.info ( "Pong!" );
			event.getChannel ().sendMessage ( "Pong!" ).queue ();
			return;
		}

		if ( content.startsWith ( "/play " ) ) {
			log.info ( "Playing: {}", content );
			var filePath = content.substring ( 6 ); // 6 è la lunghezza di "/play "
			filePath = filePath.trim ();
			var file = ASSETS_DIR + filePath + ".mp3"; // Aggiungi l'estensione ".mp3"
			audioPlayer.playAudio ( event, file );
			return;
		}

		if ( content.equals ( "/stop" ) ) {
			log.info ( "Stopping audio playback" );
			audioPlayer.stopAudio ( event );
			return;
		}

		if ( content.startsWith ( "/list" ) ) {
			log.info ( "Listing all commands " );
			event.getChannel ().sendMessage ( "/ping - pong\n /play <audio file>\n /stop\n /list this list" ).queue ();
		}
	}

}
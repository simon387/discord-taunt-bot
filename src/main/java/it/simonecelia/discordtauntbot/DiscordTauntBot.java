package it.simonecelia.discordtauntbot;

import it.simonecelia.discordtauntbot.audio.AudioPlayer;
import it.simonecelia.discordtauntbot.text.TextSender;
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

	private final TextSender textSender;

	private final String adminID;

	private boolean verbose;

	public DiscordTauntBot ( String adminID, boolean verbose ) {
		textSender = new TextSender ();
		var currentPath = new File ( "" ).getAbsolutePath ();
		log.info ( "App working dir is: {}", currentPath );
		audioPlayer = new AudioPlayer ( currentPath + File.separator + "assets" + File.separator );
		this.adminID = adminID;
		log.info ( "Admin ID is: {}", this.adminID );
		this.verbose = verbose;
		log.info ( "Verbose is: {}", this.verbose );
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
			properties.load ( new FileInputStream ( "src/main/resources/application.properties" ) );
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

	@Override
	public void onMessageReceived ( MessageReceivedEvent event ) {
		if ( event.getAuthor ().isBot () ) {
			return;
		}
		var message = event.getMessage ();
		var content = message.getContentRaw ().trim ();

		log.info ( "Got message from: {}", event.getAuthor () );
		log.info ( "Content: {}", content );

		if ( content.startsWith ( "/p " ) || content.startsWith ( "/play " ) ) {
			audioPlayer.playAudio ( event, content, this.verbose );
			return;
		}

		switch ( content ) {
		case "/stop" -> audioPlayer.stopAudio ( event, this.verbose );
		case "/tauntlist" -> textSender.sendTauntList ( event );
		case "/links" -> textSender.sendLinks ( event );
		case "/list" -> textSender.sendCmdList ( event );
		case "/verbose" -> {
			if ( isAdmin ( event ) ) {
				this.verbose = !this.verbose;
				log.info ( "Verbose enabled: {}", this.verbose );
				event.getChannel ().sendMessage ( "Verbose enabled:" + this.verbose ).queue ();
			}
		}
		}
	}

	private boolean isAdmin ( MessageReceivedEvent event ) {
		log.info ( "Author {} is admin!", event.getAuthor () );
		return event.getAuthor ().getId ().equals ( adminID );
	}

}

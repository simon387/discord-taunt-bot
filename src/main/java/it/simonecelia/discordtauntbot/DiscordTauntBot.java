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
	@SuppressWarnings ( "HttpUrlsUsage" )
	public void onMessageReceived ( MessageReceivedEvent event ) {
		if ( event.getAuthor ().isBot () ) {
			return;
		}
		var message = event.getMessage ();
		var content = message.getContentRaw ().trim ();

		log.info ( "Got message from: {}", event.getAuthor () );
		log.info ( "Content: {}", content );

		if ( content.startsWith ( "/p " ) ) {
			log.info ( "Playing: {}", content );
			audioPlayer.playAudio ( event, ASSETS_DIR + content.substring ( 3 ).trim () + ".mp3" );
			return;
		}

		switch ( content ) {
		case "/ping" -> {
			log.info ( "Pong!" );
			event.getChannel ().sendMessage ( "Pong!" ).queue ();
		}
		case "/stop" -> {
			log.info ( "Stopping audio playback" );
			audioPlayer.stopAudio ( event );
		}
		case "/tauntlist" -> {
			log.info ( "Showing tauntlist" );
			event.getChannel ().sendMessage ( "https://www.simonecelia.it/ts-bot-web/index.html" ).queue ();
		}
		case "/links" -> {
			log.info ( "Showing links" );
			var list = new StringBuilder ();
			list.append ( "https://eden.leryk.ovh/alchemy-leveling/\n" );
			list.append ( "https://apothecary.daoc-sites.info/reference_reactives.php\n" );
			list.append ( "https://www.darkageofcamelot.com/content/spellcraft-armor-bonuses\n" );
			list.append ( "https://camelot.allakhazam.com/spellcraftcalc.html\n" );
			list.append ( "http://tool.excidio.net/spelldamage.htm\n" );
			list.append ( "https://eden-daoc.net/herald?n=top_lwrp&c=hunter\n" );
			event.getChannel ().sendMessage ( list ).queue ();
		}
		case "/list" -> {
			log.info ( "Listing all commands" );
			var list = new StringBuilder ();
			list.append ( "`/ping`         -->   pong!\n" );
			list.append ( "`/p    <taunt>` -->   plays taunt\n" );
			list.append ( "`/stop`         -->   stops all audios\n" );
			list.append ( "`/tauntlist`    -->   shows taunt list\n" );
			list.append ( "`/links`        -->   shows links\n" );
			list.append ( "`/list`         -->   shows this list\n" );
			event.getChannel ().sendMessage ( list ).queue ();
		}
		}
	}

}

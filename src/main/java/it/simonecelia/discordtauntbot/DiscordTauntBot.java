package it.simonecelia.discordtauntbot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;


public class DiscordTauntBot extends ListenerAdapter {

	private static final Logger log = LoggerFactory.getLogger ( DiscordTauntBot.class );

	private final AudioPlayerManager playerManager;

	private final TrackScheduler trackScheduler;

	private final String ASSETS_DIR;

	public DiscordTauntBot () {
		this.playerManager = new DefaultAudioPlayerManager ();
		AudioSourceManagers.registerLocalSource ( playerManager );
		this.trackScheduler = new TrackScheduler ( playerManager.createPlayer () );
		//
		var currentPath = new File ( "" ).getAbsolutePath ();
		log.info ( "App working dir is: {}", currentPath );
		ASSETS_DIR = currentPath + "\\assets";
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
			var command = content.split ( " ", 2 );
			if ( command.length == 2 ) {
				var file = ASSETS_DIR + command[1] + "mp3";
				playAudio ( event, file );
			}
			return;
		}

		if ( content.startsWith ( "/list" ) ) {
			log.info ( "Listing all commands " );
			event.getChannel ().sendMessage ( "/ping - pong\n /play <audio file>\n /list this list" ).queue ();
		}
	}

	private void playAudio ( MessageReceivedEvent event, String audioFile ) {
		var voiceChannel = Objects.requireNonNull (
						Objects.requireNonNull ( Objects.requireNonNull ( event.getMember () ).getVoiceState () ).getChannel () ).asVoiceChannel ();

		var audioManager = event.getGuild ().getAudioManager ();
		audioManager.setSendingHandler ( new AudioPlayerSendHandler ( trackScheduler.getPlayer () ) );
		audioManager.openAudioConnection ( voiceChannel );

		playerManager.loadItem ( audioFile, new AudioLoadResultHandler () {

			@Override
			public void trackLoaded ( AudioTrack track ) {
				trackScheduler.queue ( track );
				log.info ( "Riproduzione di: {}", audioFile );
				event.getChannel ().sendMessage ( "Riproduzione di: " + audioFile ).queue ();
			}

			@Override
			public void playlistLoaded ( AudioPlaylist playlist ) {
				// Non gestito per semplicità
			}

			@Override
			public void noMatches () {
				log.error ( "File audio non trovato: {}", audioFile );
				event.getChannel ().sendMessage ( "File audio non trovato: " + audioFile ).queue ();
			}

			@Override
			public void loadFailed ( FriendlyException exception ) {
				log.error ( "Errore nel caricamento del file audio: {}", exception.getMessage () );
				event.getChannel ().sendMessage ( "Errore nel caricamento del file audio: " + exception.getMessage () ).queue ();
			}
		} );
	}
}
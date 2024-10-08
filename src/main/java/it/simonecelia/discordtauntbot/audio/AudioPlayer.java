package it.simonecelia.discordtauntbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;


public class AudioPlayer {

	private static final Logger log = LoggerFactory.getLogger ( AudioPlayer.class );

	private final AudioPlayerManager playerManager;

	private final TrackScheduler trackScheduler;

	private final String ASSETS_DIR;

	public AudioPlayer ( String assetsDir ) {
		this.ASSETS_DIR = assetsDir;
		this.playerManager = new DefaultAudioPlayerManager ();
		AudioSourceManagers.registerLocalSource ( playerManager );
		this.trackScheduler = new TrackScheduler ( playerManager.createPlayer () );
	}

	public void playAudio ( MessageReceivedEvent event, String content, boolean verbose ) {
		var begindIndex = content.startsWith ( "/p " ) ? 3 : 6;
		var audioFile = ASSETS_DIR + content.substring ( begindIndex ).trim () + ".mp3";
		log.info ( "Playing: {}", audioFile );

		var voiceChannel = Objects.requireNonNull (
						Objects.requireNonNull ( Objects.requireNonNull ( event.getMember () ).getVoiceState () ).getChannel () ).asVoiceChannel ();

		var audioManager = event.getGuild ().getAudioManager ();
		audioManager.setSendingHandler ( new AudioPlayerSendHandler ( trackScheduler.getPlayer () ) );
		audioManager.openAudioConnection ( voiceChannel );

		var file = new File ( audioFile );
		var fileName = file.getName ();
		int lastDotIndex = fileName.lastIndexOf ( '.' );
		var fileNameWithoutExtension = ( lastDotIndex == -1 ) ? fileName : fileName.substring ( 0, lastDotIndex );

		playerManager.loadItem ( audioFile, new AudioLoadResultHandler () {

			@Override
			public void trackLoaded ( AudioTrack track ) {
				trackScheduler.getPlayer ().stopTrack ();
				trackScheduler.queue ( track );
				log.info ( "Playing: {}", audioFile );
				if ( verbose ) {
					event.getChannel ().sendMessage ( "Playing: " + fileNameWithoutExtension ).queue ();
				}
			}

			@Override
			public void playlistLoaded ( AudioPlaylist playlist ) {
				// Non gestito per semplicità
			}

			@Override
			public void noMatches () {
				log.error ( "Audio file not found: {}", audioFile );
				event.getChannel ().sendMessage ( "Audio file not found: " + fileNameWithoutExtension ).queue ();
			}

			@Override
			public void loadFailed ( FriendlyException exception ) {
				log.error ( "Error on loading audio file: {}", exception.getMessage () );
				event.getChannel ().sendMessage ( "Error on loading audio file: " + exception.getMessage () ).queue ();
			}
		} );
	}

	public void stopAudio ( MessageReceivedEvent event, boolean verbose ) {
		log.info ( "Stopping audio playback" );
		trackScheduler.getPlayer ().stopTrack ();  // Ferma la riproduzione del brano corrente

		var audioManager = event.getGuild ().getAudioManager ();  // Ottieni il canale vocale e chiudi la connessione audio
		if ( audioManager.isConnected () ) {
			audioManager.closeAudioConnection ();
		}

		if ( verbose ) {
			event.getChannel ().sendMessage ( "Audio stopped." ).queue ();
		}
	}
}

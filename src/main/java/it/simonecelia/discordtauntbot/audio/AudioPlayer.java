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

	public AudioPlayer () {
		this.playerManager = new DefaultAudioPlayerManager ();
		AudioSourceManagers.registerLocalSource ( playerManager );
		this.trackScheduler = new TrackScheduler ( playerManager.createPlayer () );
	}

	public void playAudio ( MessageReceivedEvent event, String audioFile ) {
		log.info ( "Playing: {}", audioFile );
		var voiceChannel = Objects.requireNonNull (
						Objects.requireNonNull ( Objects.requireNonNull ( event.getMember () ).getVoiceState () ).getChannel () ).asVoiceChannel ();

		var audioManager = event.getGuild ().getAudioManager ();
		audioManager.setSendingHandler ( new AudioPlayerSendHandler ( trackScheduler.getPlayer () ) );
		audioManager.openAudioConnection ( voiceChannel );

		// pulitura della stringa del file audio
		var file = new File ( audioFile );
		var fileName = file.getName ();
		int lastDotIndex = fileName.lastIndexOf ( '.' );
		var fileNameWithoutExtension = ( lastDotIndex == -1 ) ? fileName : fileName.substring ( 0, lastDotIndex );

		playerManager.loadItem ( audioFile, new AudioLoadResultHandler () {

			@Override
			public void trackLoaded ( AudioTrack track ) {
				trackScheduler.getPlayer ().stopTrack ();
				trackScheduler.queue ( track );
				log.info ( "Riproduzione di: {}", audioFile );
				event.getChannel ().sendMessage ( "Riproduzione di: " + fileNameWithoutExtension ).queue ();
			}

			@Override
			public void playlistLoaded ( AudioPlaylist playlist ) {
				// Non gestito per semplicità
			}

			@Override
			public void noMatches () {
				log.error ( "File audio non trovato: {}", audioFile );
				event.getChannel ().sendMessage ( "File audio non trovato: " + fileNameWithoutExtension ).queue ();
			}

			@Override
			public void loadFailed ( FriendlyException exception ) {
				log.error ( "Errore nel caricamento del file audio: {}", exception.getMessage () );
				event.getChannel ().sendMessage ( "Errore nel caricamento del file audio: " + exception.getMessage () ).queue ();
			}
		} );
	}

	public void stopAudio ( MessageReceivedEvent event ) {
		log.info ( "Stopping audio playback" );
		// Ferma la riproduzione del brano corrente
		trackScheduler.getPlayer ().stopTrack ();

		// Ottieni il canale vocale e chiudi la connessione audio
		var audioManager = event.getGuild ().getAudioManager ();
		if ( audioManager.isConnected () ) {
			audioManager.closeAudioConnection ();
		}

		// Invia un messaggio di conferma
		event.getChannel ().sendMessage ( "Audio fermato." ).queue ();
	}
}

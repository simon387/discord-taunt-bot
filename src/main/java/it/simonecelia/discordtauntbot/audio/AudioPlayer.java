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

	public void stopAudio ( MessageReceivedEvent event ) {
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

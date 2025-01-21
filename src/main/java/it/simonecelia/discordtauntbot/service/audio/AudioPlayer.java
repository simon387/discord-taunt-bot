package it.simonecelia.discordtauntbot.service.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.quarkus.logging.Log;
import it.simonecelia.discordtauntbot.config.AppConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.util.Objects;


@ApplicationScoped
public class AudioPlayer {

	@Inject
	AppConfig appConfig;

	private AudioPlayerManager playerManager;

	private TrackScheduler trackScheduler;

	private String assetDir;

	@PostConstruct
	public void onStartup () {
		this.assetDir = new File ( "" ).getAbsolutePath () + File.separator + "assets" + File.separator;
		this.playerManager = new DefaultAudioPlayerManager ();
		AudioSourceManagers.registerLocalSource ( playerManager );
		this.trackScheduler = new TrackScheduler ( playerManager.createPlayer () );
		Log.infof ( "voiceChannelID: %s", appConfig.getVoiceChannelId () );
	}

	public void playAudio ( MessageReceivedEvent event, String content, boolean verbose ) {
		var begindIndex = content.startsWith ( "/p " ) ? 3 : 6;
		var audioFile = assetDir + content.substring ( begindIndex ).trim () + ".mp3";
		Log.infof ( "Playing: %s", audioFile );

		var voiceChannel = Objects.requireNonNull (
						Objects.requireNonNull ( Objects.requireNonNull ( event.getMember () ).getVoiceState () ).getChannel () ).asVoiceChannel ();
		Log.infof ( "voiceChannel id: %s", voiceChannel.getId () );

		var audioManager = event.getGuild ().getAudioManager ();
		audioManager.setSendingHandler ( new AudioPlayerSendHandler ( trackScheduler.getPlayer () ) );
		audioManager.openAudioConnection ( voiceChannel );

		var file = new File ( audioFile );
		var fileName = file.getName ();
		var lastDotIndex = fileName.lastIndexOf ( '.' );
		var fileNameWithoutExtension = ( lastDotIndex == -1 ) ? fileName : fileName.substring ( 0, lastDotIndex );

		playerManager.loadItem ( audioFile, new AudioLoadResultHandler () {

			@Override
			public void trackLoaded ( AudioTrack track ) {
				trackScheduler.getPlayer ().stopTrack ();
				trackScheduler.queue ( track );
				Log.infof ( "Playing: %s", audioFile );
				if ( verbose ) {
					event.getChannel ().sendMessage ( "Playing: " + fileNameWithoutExtension ).queue ();
				}
			}

			@Override
			public void playlistLoaded ( AudioPlaylist playlist ) {
				// Not handled for simplicity
			}

			@Override
			public void noMatches () {
				Log.errorf ( "Audio file not found: %s", audioFile );
				event.getChannel ().sendMessage ( "Audio file not found: " + fileNameWithoutExtension ).queue ();
			}

			@Override
			public void loadFailed ( FriendlyException exception ) {
				Log.errorf ( "Error on loading audio file: %s", exception.getMessage () );
				event.getChannel ().sendMessage ( "Error on loading audio file: " + exception.getMessage () ).queue ();
			}
		} );
	}

	public void stopAudio ( MessageReceivedEvent event, boolean verbose ) {
		Log.info ( "Stopping audio playback" );
		trackScheduler.getPlayer ().stopTrack ();  // Stop the current track

		var audioManager = event.getGuild ().getAudioManager ();  // Get the voice channel and close the audio connection
		if ( audioManager.isConnected () ) {
			audioManager.closeAudioConnection ();
		}

		if ( verbose ) {
			event.getChannel ().sendMessage ( "Audio stopped." ).queue ();
		}
	}
}

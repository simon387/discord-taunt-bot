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


@ApplicationScoped
public class AudioPlayerService {

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
		var audioFile = assetDir + content.trim () + ".mp3";
		Log.infof ( "Playing: %s", audioFile );

		var member = event.getMember ();
		if ( member == null ) {
			Log.error ( "Message not from a guild (probably a DM)." );
			event.getAuthor ().openPrivateChannel ().queue (
							privateChannel -> privateChannel.sendMessage ( "This command can only be used in a server." ).queue (),
							error -> Log.error ( "Cannot send DM to user: " + error.getMessage () )
			);
			return;
		}

		var voiceState = member.getVoiceState ();
		if ( voiceState == null || voiceState.getChannel () == null ) {
			Log.warn ( "User is not in a voice channel." );
			return;
		}

		if ( member.getEffectiveName ().equalsIgnoreCase ( "shock" )) { //TODO temp
			event.getChannel ().sendMessage ( "Shock is banned from discord-taunt-bot!" ).queue ();
		}

		// safe: only reached if user is in a voice channel
		var voiceChannel = voiceState.getChannel ().asVoiceChannel ();
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
				// event.getChannel ().sendMessage ( "Audio file not found: " + fileNameWithoutExtension ).queue ();
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

		if ( verbose ) {
			event.getChannel ().sendMessage ( "Audio stopped." ).queue ();
		}
	}
}

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
import it.simonecelia.discordtauntbot.manager.GuildMusicManager;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@ApplicationScoped
public class AudioPlayerService {

	@Inject
	AppConfig appConfig;

	private AudioPlayerManager playerManager;

	private String assetDir;

	// 1 music manager PER GUILD
	private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<> ();

	@PostConstruct
	public void onStartup () {
		this.assetDir = new File ( "" ).getAbsolutePath () + File.separator + "assets" + File.separator;
		this.playerManager = new DefaultAudioPlayerManager ();
		AudioSourceManagers.registerLocalSource ( playerManager );
		Log.infof ( "voiceChannelID: %s", appConfig.getVoiceChannelId () );
	}

	private GuildMusicManager getGuildMusicManager ( long guildId ) {
		return musicManagers.computeIfAbsent (
						guildId,
						id -> new GuildMusicManager ( playerManager )
		);
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

		if ( member.getId ().equalsIgnoreCase ( "703341245010935909" ) || member.getEffectiveName ().equalsIgnoreCase ( "shock" ) ) {
			Log.warn ( "User is banned." );
			event.getChannel ().sendMessage ( "Shock is banned from discord-taunt-bot!" ).queue ();
			return;
		}

		var voiceChannel = voiceState.getChannel ().asVoiceChannel ();
		var guild = event.getGuild ();

		var musicManager = getGuildMusicManager ( guild.getIdLong () );

		var audioManager = guild.getAudioManager ();
		audioManager.setSendingHandler (
						new AudioPlayerSendHandler ( musicManager.getPlayer () )
		);
		audioManager.openAudioConnection ( voiceChannel );

		playerManager.loadItem ( audioFile, new AudioLoadResultHandler () {

			@Override
			public void trackLoaded ( AudioTrack track ) {
				musicManager.getPlayer ().stopTrack ();
				musicManager.getScheduler ().queue ( track );

				if ( verbose ) {
					event.getChannel ().sendMessage (
									"Playing: " + track.getInfo ().title
					).queue ();
				}
			}

			@Override
			public void playlistLoaded ( AudioPlaylist playlist ) {
				// not used
			}

			@Override
			public void noMatches () {
				Log.errorf ( "Audio file not found: %s", audioFile );
			}

			@Override
			public void loadFailed ( FriendlyException exception ) {
				Log.errorf ( "Error loading audio: %s", exception.getMessage () );
				event.getChannel ().sendMessage (
								"Error loading audio: " + exception.getMessage ()
				).queue ();
			}
		} );
	}

	public void stopAudio ( MessageReceivedEvent event, boolean verbose ) {
		var guildId = event.getGuild ().getIdLong ();
		var musicManager = musicManagers.get ( guildId );

		if ( musicManager != null ) {
			musicManager.getPlayer ().stopTrack ();
		}

		if ( verbose ) {
			event.getChannel ().sendMessage ( "Audio stopped." ).queue ();
		}
	}
}

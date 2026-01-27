package it.simonecelia.discordtauntbot.manager;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import it.simonecelia.discordtauntbot.service.audio.TrackScheduler;


public class GuildMusicManager {

	private final AudioPlayer player;

	private final TrackScheduler scheduler;

	public GuildMusicManager ( AudioPlayerManager manager ) {
		this.player = manager.createPlayer ();
		this.scheduler = new TrackScheduler ( player );
		this.player.addListener ( scheduler );
	}

	public AudioPlayer getPlayer () {
		return player;
	}

	public TrackScheduler getScheduler () {
		return scheduler;
	}
}

package it.simonecelia.discordtauntbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class TrackScheduler extends AudioEventAdapter {

	private static final Logger log = LoggerFactory.getLogger ( TrackScheduler.class );

	private final AudioPlayer player;

	private final BlockingQueue<AudioTrack> queue;

	public TrackScheduler ( AudioPlayer player ) {
		this.player = player;
		this.queue = new LinkedBlockingQueue<> ();
	}

	public void queue ( AudioTrack track ) {
		if ( !player.startTrack ( track, true ) ) {
			var added = queue.offer ( track );
			log.debug ( "Player startTrack queue offer added: {}", added );
		}
	}

	public void nextTrack () {
		player.startTrack ( queue.poll (), false );
	}

	@Override
	public void onTrackEnd ( AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason ) {
		if ( endReason.mayStartNext ) {
			nextTrack ();
		}
	}

	public AudioPlayer getPlayer () {
		return player;
	}
}
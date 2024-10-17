package it.simonecelia.discordtauntbot.scheduler;

import it.simonecelia.discordtauntbot.audio.tts.TTSSender;
import it.simonecelia.discordtauntbot.business.DTBInput;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static it.simonecelia.discordtauntbot.util.Constants.KOTH_TIMES;
import static java.util.concurrent.Executors.newScheduledThreadPool;


public class KoTHScheduler {

	private static final Logger log = LoggerFactory.getLogger ( KoTHScheduler.class );

	private final ScheduledExecutorService scheduler;

	private final TTSSender ttsSender;

	private final DTBInput input;

	public KoTHScheduler ( TTSSender ttsSender, DTBInput input, JDA jda ) {
		this.ttsSender = ttsSender;
		this.input = input;
		this.input.setJda ( jda );
		log.info ( "Current time is: {}", LocalTime.now () );
		this.scheduler = newScheduledThreadPool ( 1 );

		for ( var hour : KOTH_TIMES ) {
			scheduleTaskAt ( LocalTime.of ( hour, 0 ) );
		}
	}

	public void shutdown () {
		scheduler.shutdown ();
	}

	private void scheduleTaskAt ( LocalTime targetTime ) {  // Method to schedule a task at a specific time
		var initialDelay = Duration.between ( LocalTime.now (), targetTime ).toMillis ();
		if ( initialDelay < 0 ) {
			initialDelay += Duration.ofDays ( 1 ).toMillis ();  // If the target time is before the current time, schedule for the next day
		}
		scheduler.scheduleAtFixedRate ( this::triggeredTask, initialDelay, TimeUnit.DAYS.toMillis ( 1 ), TimeUnit.MILLISECONDS );
		log.info ( "Task scheduled to trigger at {}", targetTime );
	}

	private void triggeredTask () {  // The task to be triggered
		log.info ( "Triggered task at scheduled time: {}", LocalTime.now () );
		ttsSender.sendKotHAlertWithTTS ( input );
	}
}

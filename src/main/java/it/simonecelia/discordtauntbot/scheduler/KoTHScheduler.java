package it.simonecelia.discordtauntbot.scheduler;

import it.simonecelia.discordtauntbot.business.DTBInput;
import it.simonecelia.discordtauntbot.text.TextSender;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newScheduledThreadPool;


public class KoTHScheduler {

	private static final Logger log = LoggerFactory.getLogger ( KoTHScheduler.class );

	private final ScheduledExecutorService scheduler;

	private final TextSender textSender;

	private final DTBInput input;

	public KoTHScheduler ( TextSender textSender, DTBInput input, JDA jda ) {
		this.textSender = textSender;
		this.input = input;
		this.input.setJda ( jda );
		log.info ( "Current time is: {}", LocalTime.now () );
		this.scheduler = newScheduledThreadPool ( 1 );
		scheduleTaskAt ( LocalTime.of ( 1, 0 ) );  // Schedule the task to trigger at a specific time
		scheduleTaskAt ( LocalTime.of ( 5, 0 ) );  // E.g., Trigger the task at 17:00 (5:00 PM)
		scheduleTaskAt ( LocalTime.of ( 8, 0 ) );
		scheduleTaskAt ( LocalTime.of ( 13, 0 ) );
		scheduleTaskAt ( LocalTime.of ( 17, 0 ) );
		scheduleTaskAt ( LocalTime.of ( 21, 0 ) );
		scheduleTaskAt ( LocalTime.of ( 23, 0 ) );
		//TODO add other koth times
	}

	public void shutdown () {
		scheduler.shutdown ();
	}

	private void scheduleTaskAt ( LocalTime targetTime ) {  // Method to schedule a task at a specific time
		var now = LocalTime.now ();
		var initialDelay = Duration.between ( now, targetTime ).toMillis ();
		if ( initialDelay < 0 ) {
			initialDelay += Duration.ofDays ( 1 ).toMillis ();  // If the target time is before the current time, schedule for the next day
		}
		scheduler.scheduleAtFixedRate ( this::triggeredTask, initialDelay, TimeUnit.DAYS.toMillis ( 1 ), TimeUnit.MILLISECONDS );
		log.info ( "Task scheduled to trigger at {}", targetTime );
	}

	private void triggeredTask () {  // The task to be triggered
		log.info ( "Triggered task at scheduled time: {}", LocalTime.now () );
		textSender.sendKotHAlert ( input );
	}
}

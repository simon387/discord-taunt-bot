package it.simonecelia.discordtauntbot.scheduler;

import io.quarkus.logging.Log;
import it.simonecelia.discordtauntbot.config.AppConfig;
import it.simonecelia.discordtauntbot.service.audio.tts.TTSSender;
import it.simonecelia.discordtauntbot.enums.KothTimesEnum;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newScheduledThreadPool;


@ApplicationScoped
public class KoTHScheduler {

	@Inject
	TTSSender ttsSender;

	@Inject
	AppConfig appConfig;

	private ScheduledExecutorService scheduler;

	@PostConstruct
	public void onStartup () {
		Log.infof ( "Current time is: %s", LocalTime.now () );
		this.scheduler = newScheduledThreadPool ( 1 );
		if ( appConfig.isKothEnabled () ) {
			for ( var k : KothTimesEnum.values () ) {
				scheduleTaskAt ( LocalTime.of ( k.getHours (), k.getMinutes () ) );
			}
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
		Log.infof ( "Task scheduled to trigger at %s", targetTime );
	}

	private void triggeredTask () {  // The task to be triggered
		Log.infof ( "Triggered task at scheduled time: %s", LocalTime.now () );
		ttsSender.sendKotHAlertWithTTS ();
	}
}

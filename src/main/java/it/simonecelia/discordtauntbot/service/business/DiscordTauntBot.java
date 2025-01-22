package it.simonecelia.discordtauntbot.service.business;

import io.quarkus.logging.Log;
import it.simonecelia.discordtauntbot.service.audio.AudioPlayerService;
import it.simonecelia.discordtauntbot.service.download.DownloadService;
import it.simonecelia.discordtauntbot.service.text.TextSenderService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


@ApplicationScoped
public class DiscordTauntBot extends DiscordTauntBotBaseLogger {

	@Inject
	AudioPlayerService audioPlayer;

	@Inject
	TextSenderService textSender;

	@Inject
	DownloadService downloadService;

	@PostConstruct
	public void onStartup () {
		Log.infof ( "Admin ID is: %s", appConfig.getAdminId () );
		Log.infof ( "Verbose is: %s", appConfig.isVerbose () );
	}

	@Override
	public void onMessageReceived ( MessageReceivedEvent event ) {
		if ( event.getAuthor ().isBot () ) {
			return;
		}
		var message = event.getMessage ();
		var content = message.getContentRaw ().trim ();
		Log.infof ( "Got message from: %s, with Content: %s", event.getAuthor (), content );

		switch ( content ) {
		case String c when c.startsWith ( "/p " ) || c.startsWith ( "/play " ) -> audioPlayer.playAudio ( event, content, appConfig.isVerbose () );
		case String c when c.startsWith ( "/tts " ) -> ttsSender.sendTTS ( event, content );
		case "/stop" -> audioPlayer.stopAudio ( event, appConfig.isVerbose () );
		case "/tauntlist" -> textSender.sendTauntList ( event );
		case "/links" -> textSender.sendLinks ( event );
		case "/list" -> textSender.sendCmdList ( event );
		case "/koth" -> textSender.getTimeUntilNextKothTime ( event );
		case "/version" -> textSender.sendVersion ( event );
		case "/verbose" -> {
			if ( isFromAdmin ( event ) ) {
				appConfig.setVerbose ( !appConfig.isVerbose () );
				Log.infof ( "Verbose enabled: %s", appConfig.isVerbose () );
				event.getChannel ().sendMessage ( "Verbose enabled:" + appConfig.isVerbose () ).queue ();
			}
		}
		case "/kill" -> {
			if ( isFromAdmin ( event ) ) {
				koTHScheduler.shutdown ();
				System.exit ( 0 );
			}
		}
		case "/guide" -> textSender.sendCraftingGuide ( event );
		default -> downloadService.attemptDownload ( content );
		}
	}

}

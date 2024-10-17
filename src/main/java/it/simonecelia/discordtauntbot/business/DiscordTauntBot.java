package it.simonecelia.discordtauntbot.business;

import it.simonecelia.discordtauntbot.audio.AudioPlayer;
import it.simonecelia.discordtauntbot.dto.DTBInputDTO;
import it.simonecelia.discordtauntbot.text.TextSender;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class DiscordTauntBot extends DiscordTauntBotBaseLogger {

	private static final Logger log = LoggerFactory.getLogger ( DiscordTauntBot.class );

	private final AudioPlayer audioPlayer;

	private final TextSender textSender;

	public DiscordTauntBot ( DTBInputDTO input ) {
		super ( input );
		textSender = new TextSender ();
		var currentPath = new File ( "" ).getAbsolutePath ();
		audioPlayer = new AudioPlayer ( currentPath );
		log.info ( "App working dir is: {}", currentPath );
		log.info ( "Admin ID is: {}", input.getAdminID () );
		log.info ( "Verbose is: {}", input.isVerbose () );
	}

	@Override
	public void onMessageReceived ( MessageReceivedEvent event ) {
		if ( event.getAuthor ().isBot () ) {
			return;
		}
		var message = event.getMessage ();
		var content = message.getContentRaw ().trim ();
		log.info ( "Got message from: {}, with Content: {}", event.getAuthor (), content );

		switch ( content ) {
		case String c when c.startsWith ( "/p " ) || c.startsWith ( "/play " ) -> audioPlayer.playAudio ( event, content, this.input.isVerbose () );
		case String c when c.startsWith ( "/tts " ) -> ttsSender.sendTTS ( event, content );
		case "/stop" -> audioPlayer.stopAudio ( event, this.input.isVerbose () );
		case "/tauntlist" -> textSender.sendTauntList ( event );
		case "/links" -> textSender.sendLinks ( event );
		case "/list" -> textSender.sendCmdList ( event );
		case "/koth" -> textSender.getTimeUntilNextKothTime ( event );
		case "/version" -> textSender.sendVersion ( event );
		case "/verbose" -> {
			if ( isFromAdmin ( event ) ) {
				this.input.setVerbose ( !this.input.isVerbose () );
				log.info ( "Verbose enabled: {}", this.input.isVerbose () );
				event.getChannel ().sendMessage ( "Verbose enabled:" + this.input.isVerbose () ).queue ();
			}
		}
		case "/kill" -> {
			if ( isFromAdmin ( event ) ) {
				koTHScheduler.shutdown ();
				System.exit ( 0 );
			}
		}
		default -> {
		}
		}
	}

}

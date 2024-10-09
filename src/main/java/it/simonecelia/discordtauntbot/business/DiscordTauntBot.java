package it.simonecelia.discordtauntbot.business;

import it.simonecelia.discordtauntbot.audio.AudioPlayer;
import it.simonecelia.discordtauntbot.text.TextSender;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class DiscordTauntBot extends ListenerAdapter {

	private static final Logger log = LoggerFactory.getLogger ( DiscordTauntBot.class );

	private final AudioPlayer audioPlayer;

	private final TextSender textSender;

	private final String adminID;

	private boolean verbose;

	public DiscordTauntBot ( String adminID, boolean verbose ) {
		textSender = new TextSender ();
		var currentPath = new File ( "" ).getAbsolutePath ();
		log.info ( "App working dir is: {}", currentPath );
		audioPlayer = new AudioPlayer ( currentPath );
		this.adminID = adminID;
		log.info ( "Admin ID is: {}", this.adminID );
		this.verbose = verbose;
		log.info ( "Verbose is: {}", this.verbose );
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
		case String c when c.startsWith ( "/p " ) || c.startsWith ( "/play " ) -> audioPlayer.playAudio ( event, content, this.verbose );
		case "/stop" -> audioPlayer.stopAudio ( event, this.verbose );
		case "/tauntlist" -> textSender.sendTauntList ( event );
		case "/links" -> textSender.sendLinks ( event );
		case "/list" -> textSender.sendCmdList ( event );
		case "/version" -> textSender.sendVersion ( event );
		case "/verbose" -> {
			if ( isFromAdmin ( event ) ) {
				this.verbose = !this.verbose;
				log.info ( "Verbose enabled: {}", this.verbose );
				event.getChannel ().sendMessage ( "Verbose enabled:" + this.verbose ).queue ();
			}
		}
		case "/kill" -> {
			if ( isFromAdmin ( event ) ) {
				System.exit ( 0 );
			}
		}
		default -> {
		}
		}
	}

	private boolean isFromAdmin ( MessageReceivedEvent event ) {
		log.info ( "Checking if Author {} is admin", event.getAuthor () );
		return event.getAuthor ().getId ().equals ( adminID );
	}

}

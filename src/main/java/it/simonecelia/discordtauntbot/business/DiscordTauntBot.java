package it.simonecelia.discordtauntbot.business;

import it.simonecelia.discordtauntbot.audio.AudioPlayer;
import it.simonecelia.discordtauntbot.audio.tts.TTSSender;
import it.simonecelia.discordtauntbot.scheduler.KoTHScheduler;
import it.simonecelia.discordtauntbot.text.TextSender;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class DiscordTauntBot extends ListenerAdapter {

	private static final Logger log = LoggerFactory.getLogger ( DiscordTauntBot.class );

	private final AudioPlayer audioPlayer;

	private final TextSender textSender;

	private final TTSSender ttsSender;

	private final DTBInput input;

	private KoTHScheduler koTHScheduler;

	public DiscordTauntBot ( DTBInput input ) {
		this.input = input;
		textSender = new TextSender ();
		ttsSender = new TTSSender ();
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

	private boolean isFromAdmin ( MessageReceivedEvent event ) {
		log.info ( "Checking if Author {} is admin", event.getAuthor () );
		return event.getAuthor ().getId ().equals ( this.input.getAdminID () );
	}

	public void setSchedulerAndJDA ( JDA jda ) {
		koTHScheduler = new KoTHScheduler ( textSender, ttsSender, this.input, jda );
	}

	@Override
	public void onGuildVoiceUpdate ( @NotNull GuildVoiceUpdateEvent event ) {
		var member = event.getMember ();
		var joinedChannel = event.getChannelJoined ();
		var leftChannel = event.getChannelLeft ();

		if ( joinedChannel != null ) {
			log.info ( "{} entered voice channel: {}[id={}]", member.getEffectiveName (), joinedChannel.getName (), joinedChannel.getId () );
		}

		if ( leftChannel != null ) {
			log.info ( "{} left voice channel: {}[id={}]", member.getEffectiveName (), leftChannel.getName (), leftChannel.getId () );
		}

		if ( joinedChannel != null && leftChannel != null ) {
			log.info ( "{} moved from voice channel {}[id={}] to {}[id={}]",
							member.getEffectiveName (),
							leftChannel.getName (),
							leftChannel.getId (),
							joinedChannel.getName (),
							joinedChannel.getId () );
		}
	}
}

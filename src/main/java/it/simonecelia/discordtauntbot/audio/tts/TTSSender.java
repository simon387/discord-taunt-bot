package it.simonecelia.discordtauntbot.audio.tts;

import it.simonecelia.discordtauntbot.dto.DTBInputDTO;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TTSSender {

	private static final Logger log = LoggerFactory.getLogger ( TTSSender.class );

	// TTS WORKS ON TEXT-CHANNELS, DO NOT WORKS ON VOICE CHANNELS !!
	public void sendKotHAlertWithTTS ( DTBInputDTO input ) {
		log.info ( "Attempting to send KoTH alert with TTS. guildId = {}, channelId = {}", input.getGuildID (), input.getChannelID () );

		if ( input.getGuildID () == null ) {
			log.error ( "Guild ID is null." );
			return;
		}

		var guild = input.getJda ().getGuildById ( input.getGuildID () );
		if ( guild == null ) {
			log.error ( "Guild not found, ID: {}", input.getGuildID () );
			return;
		}

		var textChannel = guild.getTextChannelById ( input.getChannelID () );
		if ( textChannel != null ) {
			textChannel.sendMessage ( "KoTH event has started!" )
							.setTTS ( true ) // enable TTS
							.queue ();
			log.info ( "TTS message sent to channel: {}", textChannel.getName () );
		} else {
			log.error ( "Channel not found, ID: {}", input.getChannelID () );
		}
	}

	public void sendTTS ( MessageReceivedEvent event, String content ) {
		var text = content.substring ( 5 ).trim ();
		log.info ( "Sending TTS message : {}", text );
		event.getChannel ().sendMessage ( text ).setTTS ( true ).queue ();
	}
}

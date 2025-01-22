package it.simonecelia.discordtauntbot.service.audio.tts;

import io.quarkus.logging.Log;
import it.simonecelia.discordtauntbot.config.AppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


@ApplicationScoped
public class TTSSender {

	@Inject
	AppConfig appConfig;

	// TTS WORKS ON TEXT-CHANNELS, DO NOT WORKS ON VOICE CHANNELS !!
	public void sendKotHAlertWithTTS () {
		Log.infof ( "Attempting to send KoTH alert with TTS. guildId = %s, channelId = %s", appConfig.getGuildId (), appConfig.getChannelId () );

		if ( appConfig.getGuildId () == null ) {
			Log.error ( "Guild ID is null." );
			return;
		}

		var guild = appConfig.getJda ().getGuildById ( appConfig.getGuildId () );
		if ( guild == null ) {
			Log.errorf ( "Guild not found, ID: %s", appConfig.getGuildId () );
			return;
		}

		var textChannel = guild.getTextChannelById ( appConfig.getChannelId () );
		if ( textChannel != null ) {
			textChannel.sendMessage ( "KoTH event has started!" )
							.setTTS ( true ) // enable TTS
							.queue ();
			Log.infof ( "TTS message sent to channel: %s", textChannel.getName () );
		} else {
			Log.errorf ( "Channel not found, ID: %s", appConfig.getChannelId () );
		}
	}

	public void sendTTS ( MessageReceivedEvent event, String content ) {
		var text = content.substring ( 5 ).trim ();
		Log.infof ( "Sending TTS message : %s", text );
		event.getChannel ().sendMessage ( text ).setTTS ( true ).queue ();
	}
}

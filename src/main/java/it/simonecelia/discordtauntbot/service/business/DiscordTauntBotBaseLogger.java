package it.simonecelia.discordtauntbot.service.business;

import io.quarkus.logging.Log;
import it.simonecelia.discordtauntbot.config.AppConfig;
import it.simonecelia.discordtauntbot.service.audio.tts.TTSSender;
import it.simonecelia.discordtauntbot.scheduler.KoTHScheduler;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;


public class DiscordTauntBotBaseLogger extends ListenerAdapter {

	protected KoTHScheduler koTHScheduler;

	@Inject
	TTSSender ttsSender;

	@Inject
	AppConfig appConfig;

	protected boolean isFromAdmin ( MessageReceivedEvent event ) {
		Log.infof ( "Checking if Author %s is admin", event.getAuthor () );
		return event.getAuthor ().getId ().equals ( appConfig.getAdminId () );
	}

	@Override
	public void onGuildVoiceUpdate ( @NotNull GuildVoiceUpdateEvent event ) {
		var member = event.getMember ();
		var joinedChannel = event.getChannelJoined ();
		var leftChannel = event.getChannelLeft ();

		if ( joinedChannel != null ) {
			Log.infof ( "%s entered voice channel: %s[id=%s]", member.getEffectiveName (), joinedChannel.getName (), joinedChannel.getId () );
		}

		if ( leftChannel != null ) {
			Log.infof ( "%s left voice channel: %s[id=%s]", member.getEffectiveName (), leftChannel.getName (), leftChannel.getId () );
		}

		if ( joinedChannel != null && leftChannel != null ) {
			Log.infof ( "%s moved from voice channel %s[id=%s] to %s[id=%s]",
							member.getEffectiveName (),
							leftChannel.getName (),
							leftChannel.getId (),
							joinedChannel.getName (),
							joinedChannel.getId () );
		}
	}
}

package it.simonecelia.discordtauntbot.business;

import it.simonecelia.discordtauntbot.audio.tts.TTSSender;
import it.simonecelia.discordtauntbot.scheduler.KoTHScheduler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DiscordTauntBotBaseLogger extends ListenerAdapter {

	private static final Logger log = LoggerFactory.getLogger ( DiscordTauntBotBaseLogger.class );

	protected KoTHScheduler koTHScheduler;

	protected final DTBInput input;

	protected final TTSSender ttsSender;

	public DiscordTauntBotBaseLogger ( DTBInput input ) {
		this.input = input;
		ttsSender = new TTSSender ();
	}

	protected boolean isFromAdmin ( MessageReceivedEvent event ) {
		log.info ( "Checking if Author {} is admin", event.getAuthor () );
		return event.getAuthor ().getId ().equals ( this.input.getAdminID () );
	}

	public void setSchedulerAndJDA ( JDA jda ) {
		koTHScheduler = new KoTHScheduler ( ttsSender, this.input, jda );
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

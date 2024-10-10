package it.simonecelia.discordtauntbot.text;

import it.simonecelia.discordtauntbot.business.DTBInput;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TextSender {

	private static final Logger log = LoggerFactory.getLogger ( TextSender.class );

	public void sendTauntList ( MessageReceivedEvent event ) {
		log.info ( "Showing tauntlist" );
		event.getChannel ().sendMessage ( "https://www.simonecelia.it/ts-bot-web/index.html" ).queue ();
	}

	@SuppressWarnings ( "HttpUrlsUsage" )
	public void sendLinks ( MessageReceivedEvent event ) {
		log.info ( "Showing links" );
		var list = new StringBuilder ();
		list.append ( "https://eden.leryk.ovh/alchemy-leveling/\n" );
		list.append ( "https://apothecary.daoc-sites.info/reference_reactives.php\n" );
		list.append ( "https://www.darkageofcamelot.com/content/spellcraft-armor-bonuses\n" );
		list.append ( "https://camelot.allakhazam.com/spellcraftcalc.html\n" );
		list.append ( "http://tool.excidio.net/spelldamage.htm\n" );
		list.append ( "https://eden-daoc.net/herald?n=top_lwrp&c=hunter\n" );
		event.getChannel ().sendMessage ( list ).queue ();
	}

	public void sendCmdList ( MessageReceivedEvent event ) {
		log.info ( "Listing all commands" );
		var list = new StringBuilder ();
		list.append ( "`/play <taunt>  -->   plays taunt`\n" );
		list.append ( "`/p    <taunt>  -->   plays taunt`\n" );
		list.append ( "`/stop          -->   stops all audios`\n" );
		list.append ( "`/tauntlist     -->   shows taunt list`\n" );
		list.append ( "`/links         -->   shows links`\n" );
		list.append ( "`/list          -->   shows this list`\n" );
		list.append ( "`/version       -->   shows version infos`\n" );
		list.append ( "`/verbose       -->   switch verbose flag on/off (only admins)`\n" );
		list.append ( "`/kill          -->   kills the bot (only admins)`\n" );
		event.getChannel ().sendMessage ( list ).queue ();
	}

	public void sendVersion ( MessageReceivedEvent event ) {
		log.info ( "Showing version" );
		event.getChannel ().sendMessage ( "https://github.com/simon387/discord-taunt-bot/blob/master/changelog.txt" ).queue ();
	}

	public void sendKotHAlert ( DTBInput input ) {
		log.info ( "Showing KoTH alert,  guiildId = {}, channelId = {}", input.getGuildID (), input.getChannelID () );
		var guild = input.getJda ().getGuildById ( input.getGuildID () );
		if ( input.getGuildID () != null ) {
			assert guild != null;
			var textChannel = guild.getTextChannelById ( input.getChannelID () );
			if ( textChannel != null ) {
				textChannel.sendMessage ( "KoTH event has started!" ).queue (); // queue() is asynchronous, so it doesn't block the flow
				log.info ( "Text send to channel: {}", textChannel.getName () );
			} else {
				log.error ( "Channel not found, ID: {}", input.getChannelID () );
			}
		} else {
			log.error ( "Guild not found, ID: {}", input.getGuildID () );
		}
	}
}

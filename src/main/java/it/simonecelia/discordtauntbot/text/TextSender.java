package it.simonecelia.discordtauntbot.text;

import it.simonecelia.discordtauntbot.enums.KothTimesEnum;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


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
		list.append ( "Alchemy guide: https://eden.leryk.ovh/alchemy-leveling/\n" );
		list.append ( "Alchemy reference: https://apothecary.daoc-sites.info/reference_reactives.php\n" );
		list.append ( "Armor 5th sloths: https://www.darkageofcamelot.com/content/spellcraft-armor-bonuses\n" );
		list.append ( "Spellcraft calc: https://camelot.allakhazam.com/spellcraftcalc.html\n" );
		list.append ( "Top Spears: https://eden-daoc.net/items?m=market&s=arcanium&r=2&t=14-0&f0=a-217\n" );
		list.append ( "Top Hunters: https://eden-daoc.net/herald?n=top_lwrp&c=hunter\n" );
		list.append ( "Spelldamage table: http://tool.excidio.net/spelldamage.htm\n" );
		event.getChannel ().sendMessage ( list ).queue ();
	}

	public void sendCmdList ( MessageReceivedEvent event ) {
		log.info ( "Listing all commands" );
		var list = new StringBuilder ();
		list.append ( "`/play <taunt>  -->   plays taunt`\n" );
		list.append ( "`/p    <taunt>  -->   plays taunt`\n" );
		list.append ( "`/tts  <text>   -->   send tts audio`\n" );
		list.append ( "`/stop          -->   stops all audios`\n" );
		list.append ( "`/tauntlist     -->   shows taunt list`\n" );
		list.append ( "`/links         -->   shows links`\n" );
		list.append ( "`/list          -->   shows this list`\n" );
		list.append ( "`/koth          -->   shows how many minutes are left until the KoTH`\n" );
		list.append ( "`/version       -->   shows version infos`\n" );
		list.append ( "`/verbose       -->   switch verbose flag on/off (only admins)`\n" );
		list.append ( "`/kill          -->   kills the bot (only admins)`\n" );
		event.getChannel ().sendMessage ( list ).queue ();
	}

	public void sendVersion ( MessageReceivedEvent event ) {
		log.info ( "Showing version" );
		event.getChannel ().sendMessage ( "https://github.com/simon387/discord-taunt-bot/blob/master/changelog.txt" ).queue ();
	}

	public void getTimeUntilNextKothTime ( MessageReceivedEvent event ) {
		log.info ( "Showing how many minutes are left until the KoTH" );
		List<LocalTime> scheduledTimes = new ArrayList<> ();

		for ( var k : KothTimesEnum.values () ) {
			scheduledTimes.add ( LocalTime.of ( k.getHours (), k.getMinutes () ) );
		}

		var shortestDuration = Duration.ofDays ( 1 );
		for ( var time : scheduledTimes ) {
			var duration = Duration.between ( LocalTime.now (), time );
			if ( duration.isNegative () ) {
				duration = duration.plusDays ( 1 );
			}
			if ( duration.compareTo ( shortestDuration ) < 0 ) {
				shortestDuration = duration;
			}
		}

		var hours = shortestDuration.toHoursPart ();
		var minutes = shortestDuration.toMinutesPart ();

		event.getChannel ().sendMessage ( "Time left until KoTH: " + hours + " hours and " + minutes + " minutes" ).queue ();
	}

}

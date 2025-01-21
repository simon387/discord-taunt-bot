package it.simonecelia.discordtauntbot.service.text;

import io.quarkus.logging.Log;
import it.simonecelia.discordtauntbot.enums.KothTimesEnum;
import jakarta.enterprise.context.ApplicationScoped;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@ApplicationScoped
public class TextSender {

	public void sendTauntList ( MessageReceivedEvent event ) {
		Log.info ( "Showing tauntlist" );
		event.getChannel ().sendMessage ( "https://www.simonecelia.it/ts-bot-web/index.html" ).queue ();
	}

	@SuppressWarnings ( "HttpUrlsUsage" )
	public void sendLinks ( MessageReceivedEvent event ) {
		Log.info ( "Showing links" );
		var list = new StringBuilder ();
		list.append ( "Alchemy guide: https://eden.leryk.ovh/alchemy-leveling/\n" );
		list.append ( "Alchemy reference: https://apothecary.daoc-sites.info/reference_reactives.php\n" );
		list.append ( "Armor 5th sloths: https://www.darkageofcamelot.com/content/spellcraft-armor-bonuses\n" );
		list.append ( "Spellcraft calc: https://camelot.allakhazam.com/spellcraftcalc.html\n" );
		list.append ( "Top Spears: https://eden-daoc.net/items?m=market&s=arcanium&r=2&t=14-0&f0=a-217\n" );
		list.append ( "Top Hunters: https://eden-daoc.net/herald?n=top_lwrp&c=hunter\n" );
		list.append ( "Spelldamage table: http://tool.excidio.net/spelldamage.htm\n" );
		list.append ( "Melee damage formulas: https://camelotherald.fandom.com/wiki/Melee_Damage\n\n" );
		event.getChannel ().sendMessage ( list ).queue ();
	}

	public void sendCmdList ( MessageReceivedEvent event ) {
		Log.info ( "Listing all commands" );
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
		list.append ( "`/guide         -->   shows crafting guide`\n" );
		event.getChannel ().sendMessage ( list ).queue ();
	}

	public void sendVersion ( MessageReceivedEvent event ) {
		Log.info ( "Showing version" );
		event.getChannel ().sendMessage ( "https://github.com/simon387/discord-taunt-bot/blob/master/changelog.txt" ).queue ();
	}

	public void getTimeUntilNextKothTime ( MessageReceivedEvent event ) {
		Log.info ( "Showing how many minutes are left until the KoTH" );
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

	public void sendCraftingGuide ( MessageReceivedEvent event ) {
		Log.info ( "Showing Crafting Guide" );
		var list = new StringBuilder ();
		list.append ( "`Spellcrafting:`\n" );
		list.append ( "`1) begin with Earthen essence gem (lvl21) until 21.`\n" );
		list.append ( "`2) switch to earthen war sigil (39) - until lvl 45`\n" );
		list.append ( "`3) switch to earthen evocation sigil (lvl64) until lvl 64`\n" );
		list.append ( "`4) switch to eathen fervor sigil (76) until level 80`\n" );
		list.append ( "`5) switch to earthen shielding gem (99) until 100+`\n" );
		list.append ( "\n" );
		list.append ( "`Alchemy:`\n" );
		list.append ( "`1-350ish        do red/orange poisons and dyes when no poisons available. Craft til yellow or blue here, doesn't matter much.`\n" );
		list.append ( "`350-410ish      weak elixir of healing`\n" );
		list.append ( "`410ish-520ish   poisons again, whichever are red/oj up to lifebane, make lifebane until grey`\n" );
		list.append ( "`520-555ish      stable spirit alloy tincture`\n" );
		list.append ( "`555ish-605ish   improved elixir of power`\n" );
		list.append ( "`605-665ish      stable fire fine alloy tincture`\n" );
		list.append ( "`665-700         volatile cold fine alloy weapon tincture`\n" );
		list.append ( "`700-780         crafted weapon luster remover, take this all the way to grey`\n" );
		list.append ( "`780-800         stable shard adamantium tincture`\n" );
		list.append ( "`800-845ish      regular dyes`\n" );
		list.append ( "`845-901?        unique dyes`\n" );
		list.append ( "`901?-1000       draught of strength (NOT greater)`\n" );
		list.append ( "`1000-1025ish    dark violet weapon luster`\n" );
		list.append ( "`1025+           draught of might (NOT greater)`\n" );
		event.getChannel ().sendMessage ( list ).queue ();
	}
}

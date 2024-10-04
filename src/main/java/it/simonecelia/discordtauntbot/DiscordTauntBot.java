package it.simonecelia.discordtauntbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class DiscordTauntBot extends ListenerAdapter {

	public static void main ( String[] args ) {
		var properties = new Properties ();
		try {
			properties.load ( new FileInputStream ( "src/main/resources/secret.properties" ) );
			var token = properties.getProperty ( "discord.bot.token" );

			if ( token == null || token.isEmpty () ) {
				System.out.println ( "Il token del bot non è stato trovato nel file properties." );
				return;
			}

			// Crea un'istanza di JDA con gli intenti necessari
			var builder = JDABuilder.createDefault ( token );
			builder.enableIntents ( GatewayIntent.MESSAGE_CONTENT );

			// Aggiungi il listener che gestirà i messaggi
			builder.addEventListeners ( new DiscordTauntBot () );

			// Avvia il bot
			builder.build ();
		} catch ( IOException e ) {
			e.printStackTrace ();
		}
	}

	@Override
	public void onMessageReceived ( MessageReceivedEvent event ) {
		// Ignora i messaggi del bot stesso
		if ( event.getAuthor ().isBot () ) {
			return;
		}

		var message = event.getMessage ();
		var content = message.getContentRaw ();

		// Se qualcuno scrive "!ping", rispondi con "Pong!"
		if ( content.equals ( "!ping" ) ) {
			event.getChannel ().sendMessage ( "Pong!" ).queue ();
		}
	}
}

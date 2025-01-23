package it.simonecelia.discordtauntbot.config;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import net.dv8tion.jda.api.JDA;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.Properties;


@ApplicationScoped
public class AppConfig {

	@ConfigProperty ( name = "admin.id" )
	String adminId;

	@ConfigProperty ( name = "verbose" )
	boolean verbose;

	@ConfigProperty ( name = "channel.id" )
	String channelId;

	@ConfigProperty ( name = "voice.channel.id" )
	String voiceChannelId;

	@ConfigProperty ( name = "guild.id" )
	String guildId;

	@ConfigProperty ( name = "koth.enabled" )
	boolean kothEnabled;

	private JDA jda;

	private String discordBotToken;

	private String jdownloaderDeviceName;

	private String jdownloaderEmail;

	private String jdownloaderPassword;

	@PostConstruct
	public void onStartup () {
		var properties = new Properties ();
		try ( var input = AppConfig.class.getClassLoader ().getResourceAsStream ( "secret.properties" ) ) {
			if ( input == null ) {
				Log.error ( "Sorry, unable to find secret.properties" );
				return;
			}
			properties.load ( input );
			discordBotToken = properties.getProperty ( "discord.bot.token" );
			jdownloaderDeviceName = properties.getProperty ( "jdownloader.device.name" );
			jdownloaderEmail = properties.getProperty ( "jdownloader.email" );
			jdownloaderPassword = properties.getProperty ( "jdownloader.password" );
		} catch ( IOException ex ) {
			Log.error ( ex );
		}
	}

	public void setJda ( JDA jda ) {
		this.jda = jda;
	}

	public void setVerbose ( boolean verbose ) {
		this.verbose = verbose;
	}

	public String getAdminId () {
		return adminId;
	}

	public boolean isVerbose () {
		return verbose;
	}

	public String getChannelId () {
		return channelId;
	}

	public String getVoiceChannelId () {
		return voiceChannelId;
	}

	public String getGuildId () {
		return guildId;
	}

	public boolean isKothEnabled () {
		return kothEnabled;
	}

	public JDA getJda () {
		return jda;
	}

	public String getDiscordBotToken () {
		return discordBotToken;
	}

	public String getJdownloaderDeviceName () {
		return jdownloaderDeviceName;
	}

	public String getJdownloaderEmail () {
		return jdownloaderEmail;
	}

	public String getJdownloaderPassword () {
		return jdownloaderPassword;
	}
}

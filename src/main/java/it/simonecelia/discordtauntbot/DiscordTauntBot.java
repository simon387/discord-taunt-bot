package it.simonecelia.discordtauntbot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class DiscordTauntBot extends ListenerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DiscordTauntBot.class);
	private final AudioPlayerManager playerManager;
	private final TrackScheduler trackScheduler;

	public DiscordTauntBot() {
		this.playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerLocalSource(playerManager);
		this.trackScheduler = new TrackScheduler(playerManager.createPlayer());
	}

	public static void main(String[] args) {
		logger.info("Starting Discord Taunt Bot");

		var properties = new Properties();
		try {
			properties.load(new FileInputStream("src/main/resources/secret.properties"));
			var token = properties.getProperty("discord.bot.token");

			if (token == null || token.isEmpty()) {
				logger.error("Token not found!");
				return;
			}

			var builder = JDABuilder.createDefault(token);
			builder.enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES);
			builder.addEventListeners(new DiscordTauntBot());
			builder.build();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;

		var message = event.getMessage();
		var content = message.getContentRaw();

		logger.info("Got message from: {}", event.getAuthor());
		logger.info("Content: {}", content);

		if (content.equals("!ping")) {
			event.getChannel().sendMessage("Pong!").queue();
		} else if (content.startsWith("/play ")) {
			String[] command = content.split(" ", 2);
			if (command.length == 2) {
				String filePath = command[1];
				playAudio(event, filePath);
			}
		}
	}

	private void playAudio(MessageReceivedEvent event, String filePath) {
		VoiceChannel voiceChannel = Objects.requireNonNull (
						Objects.requireNonNull ( Objects.requireNonNull ( event.getMember () ).getVoiceState () ).getChannel () ).asVoiceChannel();

		AudioManager audioManager = event.getGuild().getAudioManager();
		audioManager.setSendingHandler(new AudioPlayerSendHandler(trackScheduler.getPlayer()));
		audioManager.openAudioConnection(voiceChannel);

		playerManager.loadItem(filePath, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				trackScheduler.queue(track);
				event.getChannel().sendMessage("Riproduzione di: " + filePath).queue();
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				// Non gestito per semplicità
			}

			@Override
			public void noMatches() {
				event.getChannel().sendMessage("File audio non trovato: " + filePath).queue();
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				event.getChannel().sendMessage("Errore nel caricamento del file audio: " + exception.getMessage()).queue();
			}
		});
	}
}
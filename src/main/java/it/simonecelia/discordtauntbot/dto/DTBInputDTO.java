package it.simonecelia.discordtauntbot.dto;

import net.dv8tion.jda.api.JDA;

import java.io.Serial;
import java.io.Serializable;


@SuppressWarnings ( "unused" )
public class DTBInputDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 7037141451686861046L;

	private String adminID;

	private boolean verbose;

	private String channelID;

	private String voiceChannelID;

	private String guildID;

	private JDA jda;

	public String getAdminID () {
		return adminID;
	}

	public void setAdminID ( String adminID ) {
		this.adminID = adminID;
	}

	public boolean isVerbose () {
		return verbose;
	}

	public void setVerbose ( boolean verbose ) {
		this.verbose = verbose;
	}

	public String getChannelID () {
		return channelID;
	}

	public void setChannelID ( String channelID ) {
		this.channelID = channelID;
	}

	public String getVoiceChannelID () {
		return voiceChannelID;
	}

	public void setVoiceChannelID ( String voiceChannelID ) {
		this.voiceChannelID = voiceChannelID;
	}

	public String getGuildID () {
		return guildID;
	}

	public void setGuildID ( String guildID ) {
		this.guildID = guildID;
	}

	public JDA getJda () {
		return jda;
	}

	public void setJda ( JDA jda ) {
		this.jda = jda;
	}
}

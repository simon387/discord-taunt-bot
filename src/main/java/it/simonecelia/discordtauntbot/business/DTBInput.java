package it.simonecelia.discordtauntbot.business;

import java.io.Serial;
import java.io.Serializable;


public class DTBInput implements Serializable {

	@Serial
	private static final long serialVersionUID = 7037141451686861046L;

	private String adminID;

	private boolean verbose;

	private String channelID;

	private String guildID;

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

	public String getGuildID () {
		return guildID;
	}

	public void setGuildID ( String guildID ) {
		this.guildID = guildID;
	}
}

package it.simonecelia.discordtauntbot.service.audio.recorder;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import org.jetbrains.annotations.NotNull;


public class MyAudioReceiveHandler implements AudioReceiveHandler {

	private final AudioRecorderRingBufferService recorder;

	public MyAudioReceiveHandler ( AudioRecorderRingBufferService recorder ) {
		this.recorder = recorder;
	}

	@Override
	public boolean canReceiveUser () {
		return true;
	}

	@Override
	public void handleUserAudio(UserAudio userAudio) {
		byte[] audio = userAudio.getAudioData(1.0f);

		// DEBUG: stampa ogni tanto
		if (System.currentTimeMillis() % 5000 < 100) {
			System.out.println("Audio chunk: " + audio.length + " bytes, first bytes: " +
							audio[0] + ", " + audio[1] + ", " + audio[2]);
		}

		recorder.writeToRingBuffer(audio);
	}

	@Override
	public boolean canReceiveCombined () {
		return false;
	}

	@Override
	public void handleCombinedAudio ( @NotNull net.dv8tion.jda.api.audio.CombinedAudio combinedAudio ) {
		// Non usiamo audio combinato
	}
}

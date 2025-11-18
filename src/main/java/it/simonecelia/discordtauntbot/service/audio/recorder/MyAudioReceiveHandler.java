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
	public void handleUserAudio ( UserAudio userAudio ) {
		// Scrive direttamente i byte PCM ricevuti da JDA
		recorder.writeToRingBuffer ( userAudio.getAudioData ( 1.0f ) );
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

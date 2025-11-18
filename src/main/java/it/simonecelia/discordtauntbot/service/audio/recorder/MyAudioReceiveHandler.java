package it.simonecelia.discordtauntbot.service.audio.recorder;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import org.jetbrains.annotations.NotNull;


public class MyAudioReceiveHandler implements AudioReceiveHandler {

	private final AudioRecorderRingBufferService recorder;

	public MyAudioReceiveHandler ( AudioRecorderRingBufferService recorder ) {
		this.recorder = recorder;
	}

	/**
	 * Abilita la ricezione audio utente
	 */
	@Override
	public boolean canReceiveUser () {
		return true;
	}

	/**
	 * Qui riceviamo PCM 48kHz stereo già decodificato
	 */
	@Override
	public void handleUserAudio ( UserAudio userAudio ) {
		byte[] pcm = userAudio.getAudioData ( 1.0f ); // 1.0 = volume normale
		recorder.writeToRingBuffer ( pcm );
	}

	/**
	 * Non riceviamo audio combinato
	 */
	@Override
	public boolean canReceiveCombined () {
		return false;
	}

	@Override
	public void handleCombinedAudio ( net.dv8tion.jda.api.audio.@NotNull CombinedAudio combinedAudio ) {
		// non usiamo audio combinato
	}
}

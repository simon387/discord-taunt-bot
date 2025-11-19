package it.simonecelia.discordtauntbot.service.audio.recorder;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import org.jetbrains.annotations.NotNull;

/**
 * SOLUZIONE: Usa l'audio COMBINATO invece di quello per-utente
 * Questo mixa automaticamente tutti gli utenti in un unico stream PCM
 */
public class MyAudioReceiveHandler implements AudioReceiveHandler {

	private final AudioRecorderRingBufferService recorder;

	public MyAudioReceiveHandler(AudioRecorderRingBufferService recorder) {
		this.recorder = recorder;
	}

	@Override
	public boolean canReceiveUser() {
		// Disabilita la ricezione per utente
		return false;
	}

	@Override
	public boolean canReceiveCombined() {
		// Abilita la ricezione dell'audio combinato
		return true;
	}

	@Override
	public void handleCombinedAudio(@NotNull CombinedAudio combinedAudio) {
		// Ottieni l'audio già mixato da JDA
		// getAudioData(1.0f) restituisce PCM signed 16-bit little-endian stereo a 48kHz
		byte[] audio = combinedAudio.getAudioData(1.0f);

		// DEBUG: stampa ogni tanto
		if (System.currentTimeMillis() % 5000 < 100) {
			System.out.println("Combined audio chunk: " + audio.length + " bytes, first bytes: " +
							audio[0] + ", " + audio[1] + ", " + audio[2]);
		}

		recorder.writeToRingBuffer(audio);
	}
}
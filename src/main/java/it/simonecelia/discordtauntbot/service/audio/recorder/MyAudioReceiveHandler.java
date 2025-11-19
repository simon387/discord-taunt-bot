package it.simonecelia.discordtauntbot.service.audio.recorder;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import org.jetbrains.annotations.NotNull;

/**
 * SOLUZIONE ALTERNATIVA: Tenta di ottenere più volume/qualità
 */
public class MyAudioReceiveHandler implements AudioReceiveHandler {

	private final AudioRecorderRingBufferService recorder;
	private long lastDebugTime = 0;

	public MyAudioReceiveHandler(AudioRecorderRingBufferService recorder) {
		this.recorder = recorder;
	}

	@Override
	public boolean canReceiveUser() {
		return false;
	}

	@Override
	public boolean canReceiveCombined() {
		return true;
	}

	@Override
	public void handleCombinedAudio(@NotNull CombinedAudio combinedAudio) {
		// Volume normale
		byte[] audio = combinedAudio.getAudioData(1.0f);

		// DEBUG migliorato: calcola volume effettivo
		long now = System.currentTimeMillis();
		if (now - lastDebugTime > 5000) {
			lastDebugTime = now;

			// Calcola il volume RMS dell'audio
			long sumSquares = 0;
			int samples = 0;

			for (int i = 0; i < Math.min(audio.length, 3840); i += 2) {
				if (i + 1 < audio.length) {
					// Leggi sample 16-bit little-endian
					short sample = (short) (((audio[i + 1] & 0xFF) << 8) | (audio[i] & 0xFF));
					sumSquares += (long) sample * sample;
					samples++;
				}
			}

			double rms = samples > 0 ? Math.sqrt((double) sumSquares / samples) : 0;
			double dbFS = 20 * Math.log10(rms / 32768.0); // dBFS (0 = max volume)

			System.out.printf("Combined audio: %d bytes, RMS: %.0f, Volume: %.1f dBFS, first bytes: %d, %d, %d%n",
							audio.length, rms, dbFS, audio[0], audio[1], audio[2]);
		}

		recorder.writeToRingBuffer(audio);
	}
}
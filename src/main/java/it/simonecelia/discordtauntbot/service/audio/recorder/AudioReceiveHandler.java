package it.simonecelia.discordtauntbot.service.audio.recorder;

import io.quarkus.logging.Log;
import net.dv8tion.jda.api.audio.CombinedAudio;
import org.jetbrains.annotations.NotNull;


public class AudioReceiveHandler implements net.dv8tion.jda.api.audio.AudioReceiveHandler {

	private final AudioRecorderRingBufferService recorder;

	private long lastDebugTime = 0;

	// Configurazione rilevamento silenzio
	private static final double SILENCE_THRESHOLD_DBFS = -50.0; // Soglia in dBFS

	private static final long SILENCE_TIMEOUT_MS = 300000; // 5 minuti di silenzio

	private long lastAudioActivityTime = System.currentTimeMillis ();

	private boolean isRecording = true;

	public AudioReceiveHandler ( AudioRecorderRingBufferService recorder ) {
		this.recorder = recorder;
	}

	@Override
	public boolean canReceiveUser () {
		return false;
	}

	@Override
	public boolean canReceiveCombined () {
		return true;
	}

	@Override
	public void handleCombinedAudio ( @NotNull CombinedAudio combinedAudio ) {
		var audio = combinedAudio.getAudioData ( 1.0f );

		// Calcola il volume dell'audio
		double dbFS = calculateVolume ( audio );

		long now = System.currentTimeMillis ();

		// Verifica se c'è attività audio
		if ( dbFS > SILENCE_THRESHOLD_DBFS ) {
			// C'è audio attivo
			if ( !isRecording ) {
				Log.infof ( "[AudioReceiver] Attività audio rilevata (%.1f dBFS), riprendo la registrazione", dbFS );
				isRecording = true;
			}
			lastAudioActivityTime = now;
		} else {
			// Silenzio rilevato
			long silenceDuration = now - lastAudioActivityTime;

			if ( isRecording && silenceDuration > SILENCE_TIMEOUT_MS ) {
				Log.infof ( "[AudioReceiver] Silenzio prolungato rilevato (%d minuti), pauso la registrazione",
								silenceDuration / 60000 );
				isRecording = false;
			}
		}

		// Scrivi nel buffer solo se stiamo registrando
		if ( isRecording ) {
			recorder.writeToRingBuffer ( audio );
		}

		// Debug periodico
		if ( now - lastDebugTime > 5000 ) {
			lastDebugTime = now;
			long silenceDuration = now - lastAudioActivityTime;

			Log.debugf ( "Audio: %d bytes, Volume: %.1f dBFS, Recording: %s, Silence: %d sec",
							audio.length, dbFS, isRecording, silenceDuration / 1000 );
		}
	}

	/**
	 * Calcola il volume RMS in dBFS
	 */
	private double calculateVolume ( byte[] audio ) {
		long sumSquares = 0;
		int samples = 0;

		for ( int i = 0; i < Math.min ( audio.length, 3840 ); i += 2 ) {
			if ( i + 1 < audio.length ) {
				// Leggi sample 16-bit little-endian
				short sample = (short) ( ( ( audio[i + 1] & 0xFF ) << 8 ) | ( audio[i] & 0xFF ) );
				sumSquares += (long) sample * sample;
				samples++;
			}
		}

		if ( samples == 0 ) {
			return -96.0; // Silenzio assoluto
		}

		double rms = Math.sqrt ( (double) sumSquares / samples );
		return 20 * Math.log10 ( rms / 32768.0 ); // dBFS (0 = volume massimo)
	}
}
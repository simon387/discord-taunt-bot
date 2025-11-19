package it.simonecelia.discordtauntbot.service.audio.recorder;

import io.quarkus.logging.Log;
import net.dv8tion.jda.api.audio.CombinedAudio;
import org.jetbrains.annotations.NotNull;


public class AudioReceiveHandler implements net.dv8tion.jda.api.audio.AudioReceiveHandler {

	private final AudioRecorderRingBufferService recorder;

	private long lastDebugTime = 0;

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
		// Volume normale
		var audio = combinedAudio.getAudioData ( 1.0f );

		// DEBUG migliorato: calcola volume effettivo
		long now = System.currentTimeMillis ();
		if ( now - lastDebugTime > 5000 ) {
			lastDebugTime = now;

			// Calcola il volume RMS dell'audio
			long sumSquares = 0;
			var samples = 0;

			for ( var i = 0; i < Math.min ( audio.length, 3840 ); i += 2 ) {
				if ( i + 1 < audio.length ) {
					// Leggi sample 16-bit little-endian
					var sample = (short) ( ( ( audio[i + 1] & 0xFF ) << 8 ) | ( audio[i] & 0xFF ) );
					sumSquares += (long) sample * sample;
					samples++;
				}
			}

			var rms = samples > 0 ? Math.sqrt ( (double) sumSquares / samples ) : 0;
			var dbFS = 20 * Math.log10 ( rms / 32768.0 ); // dBFS (0 = max volume)

			Log.debugf ( "Combined audio: %d bytes, RMS: %.0f, Volume: %.1f dBFS, first bytes: %d, %d, %d%n",
							audio.length, rms, dbFS, audio[0], audio[1], audio[2] );
		}

		recorder.writeToRingBuffer ( audio );
	}
}
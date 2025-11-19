package it.simonecelia.discordtauntbot.service.audio.recorder;

import io.quarkus.logging.Log;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class AudioRecorderRingBufferService {

	private static final int SAMPLE_RATE = 48000;

	private static final int CHANNELS = 2; // Stereo

	private static final int BYTES_PER_SAMPLE = 2; // 16-bit = 2 bytes

	private static final int FRAME_SIZE = CHANNELS * BYTES_PER_SAMPLE; // 4 bytes per frame

	private static final int BYTES_PER_SECOND = SAMPLE_RATE * FRAME_SIZE;

	private static final int BUFFER_SIZE = BYTES_PER_SECOND * 3600; // 1 ora

	private final byte[] ringBuffer = new byte[BUFFER_SIZE];

	private int writePos = 0;

	private boolean bufferFilledOnce = false;

	private final File output;

	// Contatori per debug
	private long totalBytesWritten = 0;

	private long lastLogTime = System.currentTimeMillis ();

	public AudioRecorderRingBufferService ( String outputName ) {
		this.output = new File ( outputName );
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor ();
		scheduler.scheduleAtFixedRate ( this::saveLastHourToWav, 5, 5, TimeUnit.MINUTES );
		Log.infof ( "[Recorder] Recording file name: %s", output.getName () );
		Log.infof ( "[Recorder] Buffer size: %d bytes (%.2f MB)", BUFFER_SIZE, BUFFER_SIZE / 1024.0 / 1024.0 );
		Log.infof ( "[Recorder] Expected format: %d Hz, %d channels, %d-bit", SAMPLE_RATE, CHANNELS, BYTES_PER_SAMPLE * 8 );
	}

	/**
	 * Scrive i chunk interi nel ring buffer
	 */
	public synchronized void writeToRingBuffer ( byte[] pcm ) {
		if ( pcm == null || pcm.length == 0 ) {
			return;
		}

		// Log periodico per debug
		totalBytesWritten += pcm.length;
		long now = System.currentTimeMillis ();
		if ( now - lastLogTime > 30000 ) { // Log ogni 30 secondi
			Log.infof ( "[Recorder] Total bytes received: %d (%.2f MB), chunk size: %d",
							totalBytesWritten, totalBytesWritten / 1024.0 / 1024.0, pcm.length );
			lastLogTime = now;
		}

		// Copia l'intero chunk
		int remaining = pcm.length;
		int offset = 0;

		while ( remaining > 0 ) {
			int spaceToEnd = ringBuffer.length - writePos;
			int toCopy = Math.min ( remaining, spaceToEnd );

			System.arraycopy ( pcm, offset, ringBuffer, writePos, toCopy );

			writePos += toCopy;
			offset += toCopy;
			remaining -= toCopy;

			if ( writePos >= ringBuffer.length ) {
				writePos = 0;
				bufferFilledOnce = true;
				Log.info ( "[Recorder] Ring buffer wrapped around" );
			}
		}
	}

	/**
	 * Salva l'ultima ora di audio in WAV
	 */
	public synchronized void saveLastHourToWav () {
		try {
			if ( writePos == 0 && !bufferFilledOnce ) {
				Log.warn ( "[Recorder] No audio data to save yet" );
				return;
			}

			byte[] audioData;
			int dataLength;

			if ( bufferFilledOnce ) {
				audioData = new byte[BUFFER_SIZE];
				dataLength = BUFFER_SIZE;
				int endChunk = BUFFER_SIZE - writePos;
				System.arraycopy ( ringBuffer, writePos, audioData, 0, endChunk );
				System.arraycopy ( ringBuffer, 0, audioData, endChunk, writePos );
			} else {
				// Allinea alla dimensione del frame per evitare frame parziali
				dataLength = ( writePos / FRAME_SIZE ) * FRAME_SIZE;
				audioData = new byte[dataLength];
				System.arraycopy ( ringBuffer, 0, audioData, 0, dataLength );
			}

			Log.infof ( "[Recorder] Saving %d bytes (%.2f seconds) to WAV",
							dataLength, (double) dataLength / BYTES_PER_SECOND );

			// TEST: Salva anche versione RAW per debug
			// saveRawPCM ( audioData );

			// Prova SENZA conversione endianness (lascia little-endian originale)
			writeWavFile ( audioData );
			Log.infof ( "[Recorder] Saved to: %s (%.2f MB)",
							output.getAbsolutePath (), output.length () / 1024.0 / 1024.0 );
		} catch ( Exception e ) {
			Log.error ( "[Recorder] Error saving WAV", e );
		}
	}

	/**
	 * Salva file RAW PCM per test con Audacity
	 */
	private void saveRawPCM ( byte[] audioData ) {
		try {
			File rawFile = new File ( output.getParent (), "test_raw.pcm" );
			java.nio.file.Files.write ( rawFile.toPath (), audioData );
			Log.infof ( "[Recorder] Saved RAW PCM to: %s (import in Audacity: File > Import > Raw Data, 48000Hz, 16-bit PCM signed, little-endian, stereo)",
							rawFile.getAbsolutePath () );
		} catch ( Exception e ) {
			Log.error ( "[Recorder] Error saving RAW PCM", e );
		}
	}

	private void writeWavFile ( byte[] audioData ) throws Exception {
		// IMPORTANTE: Ora usiamo BIG-ENDIAN perché abbiamo convertito i dati
		var format = new AudioFormat (
						AudioFormat.Encoding.PCM_SIGNED,  // Encoding
						SAMPLE_RATE,                       // Sample rate
						16,                                // Bits per sample
						CHANNELS,                          // Channels
						FRAME_SIZE,                        // Frame size
						SAMPLE_RATE,                       // Frame rate
						true                               // BIG-ENDIAN (true = big-endian)
		);

		Log.infof ( "[Recorder] Audio format: %s", format );

		long lengthInFrames = audioData.length / format.getFrameSize ();

		try ( var bais = new ByteArrayInputStream ( audioData );
						var ais = new AudioInputStream ( bais, format, lengthInFrames ) ) {
			AudioSystem.write ( ais, AudioFileFormat.Type.WAVE, output );
		}
	}

}
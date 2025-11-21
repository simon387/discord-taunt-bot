package it.simonecelia.discordtauntbot.service.audio.recorder;

import io.quarkus.logging.Log;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class AudioRecorderRingBufferService {

	private static final int SAMPLE_RATE = 48000;

	private static final int CHANNELS = 2; // Stereo

	private static final int BYTES_PER_SAMPLE = 2; // 16-bit = 2 bytes

	private static final int FRAME_SIZE = CHANNELS * BYTES_PER_SAMPLE; // 4 bytes per frame

	private static final int BYTES_PER_SECOND = SAMPLE_RATE * FRAME_SIZE;

	private static final int BUFFER_SIZE = BYTES_PER_SECOND * 3600; // 1 hour

	private final byte[] ringBuffer = new byte[BUFFER_SIZE];

	private int writePos = 0;

	private boolean bufferFilledOnce = false;

	private final File output;

	// Counters for debug
	private long totalBytesWritten = 0;

	private long lastLogTime = System.currentTimeMillis ();

	public AudioRecorderRingBufferService ( String outputName ) {
		this.output = new File ( outputName );
		// Ensure parent directory exists
		if ( output.getParentFile () != null && !output.getParentFile ().exists () ) {
			output.getParentFile ().mkdirs ();
		}
		var scheduler = Executors.newSingleThreadScheduledExecutor ();
		scheduler.scheduleAtFixedRate ( this::saveLastHourToWav, 5, 5, TimeUnit.MINUTES );
		Log.infof ( "[Recorder] Recording file name: %s", output.getAbsolutePath () );
		Log.infof ( "[Recorder] Buffer size: %d bytes (%.2f MB)", BUFFER_SIZE, BUFFER_SIZE / 1024.0 / 1024.0 );
		Log.infof ( "[Recorder] Expected format: %d Hz, %d channels, %d-bit", SAMPLE_RATE, CHANNELS, BYTES_PER_SAMPLE * 8 );
	}

	/**
	 * Writes audio chunks into the ring buffer.
	 */
	public synchronized void writeToRingBuffer ( byte[] pcm ) {
		if ( pcm == null || pcm.length == 0 ) {
			return;
		}

		// Periodic debug log
		totalBytesWritten += pcm.length;
		var now = System.currentTimeMillis ();
		if ( now - lastLogTime > 30000 ) { // Log every 30 seconds
			Log.infof ( "[Recorder] Tot bytes received: %d (%.2f MB), chunk size: %d",
							totalBytesWritten, totalBytesWritten / 1024.0 / 1024.0, pcm.length );
			lastLogTime = now;
		}

		// Copy entire chunk
		var remaining = pcm.length;
		int offset = 0;

		while ( remaining > 0 ) {
			var spaceToEnd = ringBuffer.length - writePos;
			var toCopy = Math.min ( remaining, spaceToEnd );

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
	 * Saves the last hour of audio to WAV.
	 */
	public synchronized void saveLastHourToWav () {
		try {
			Log.infof ( "[Recorder] Starting save operation to: %s", output.getAbsolutePath () );

			if ( writePos == 0 && !bufferFilledOnce ) {
				Log.warn ( "[Recorder] No audio data to save yet" );
				return;
			}

			byte[] audioData;
			int dataLength;

			if ( bufferFilledOnce ) {
				audioData = new byte[BUFFER_SIZE];
				dataLength = BUFFER_SIZE;
				var endChunk = BUFFER_SIZE - writePos;
				System.arraycopy ( ringBuffer, writePos, audioData, 0, endChunk );
				System.arraycopy ( ringBuffer, 0, audioData, endChunk, writePos );
			} else {
				// Align to frame size to avoid partial frames
				dataLength = ( writePos / FRAME_SIZE ) * FRAME_SIZE;
				audioData = new byte[dataLength];
				System.arraycopy ( ringBuffer, 0, audioData, 0, dataLength );
			}

			Log.infof ( "[Recorder] Saving %d bytes (%.2f seconds) to WAV",
							dataLength, (double) dataLength / BYTES_PER_SECOND );

			writeWavFile ( audioData );

			// Verify file exists and has content
			if ( output.exists () ) {
				Log.infof ( "[Recorder] ✓ File saved successfully: %s (%.2f MB)",
								output.getAbsolutePath (), output.length () / 1024.0 / 1024.0 );
			} else {
				Log.errorf ( "[Recorder] ✗ File does not exist after write: %s", output.getAbsolutePath () );
			}

		} catch ( Exception e ) {
			Log.errorf ( e, "[Recorder] Error saving WAV to %s", output.getAbsolutePath () ); // ⬅️ Include stacktrace
		}
	}

	/**
	 * Writes WAV file using atomic file replacement to avoid Linux file descriptor issues.
	 */
	private void writeWavFile ( byte[] audioData ) throws Exception {
		var format = new AudioFormat (
						AudioFormat.Encoding.PCM_SIGNED,
						SAMPLE_RATE,
						16,
						CHANNELS,
						FRAME_SIZE,
						SAMPLE_RATE,
						true
		);

		Log.infof ( "[Recorder] Audio format: %s", format );
		Log.infof ( "[Recorder] Writing to file: %s", output.getAbsolutePath () );

		// Write to a temporary file first (atomic operation pattern)
		var tempFile = new File ( output.getParentFile (), output.getName () + ".tmp" );

		try {
			long lengthInFrames = audioData.length / format.getFrameSize ();

			try ( var bais = new ByteArrayInputStream ( audioData );
							var ais = new AudioInputStream ( bais, format, lengthInFrames ) ) {

				// Write to temporary file
				AudioSystem.write ( ais, AudioFileFormat.Type.WAVE, tempFile );
			}

			// Verify temp file was created
			if ( !tempFile.exists () || tempFile.length () == 0 ) {
				throw new IOException ( "Temporary file was not created or is empty: " + tempFile.getAbsolutePath () );
			}

			Log.infof ( "[Recorder] Temp file written: %d bytes", tempFile.length () );

			// Atomic replace: move temp file to final destination
			// This ensures the file is completely written before replacing the old one
			Files.move (
							tempFile.toPath (),
							output.toPath (),
							StandardCopyOption.REPLACE_EXISTING,
							StandardCopyOption.ATOMIC_MOVE
			);

			Log.infof ( "[Recorder] File moved to final destination: %d bytes", output.length () );

			// Force filesystem sync on Linux
			if ( System.getProperty ( "os.name" ).toLowerCase ().contains ( "linux" ) ) {
				try {
					// This ensures the file is really written to disk
					Runtime.getRuntime ().exec ( new String[] { "sync" } );
				} catch ( IOException e ) {
					Log.warn ( "[Recorder] Could not force filesystem sync", e );
				}
			}

		} finally {
			// Clean up temp file if it still exists
			if ( tempFile.exists () ) {
				try {
					Files.delete ( tempFile.toPath () );
					Log.debug ( "[Recorder] Cleaned up temporary file" );
				} catch ( IOException e ) {
					Log.warn ( "[Recorder] Could not delete temporary file: " + tempFile.getAbsolutePath (), e );
				}
			}
		}
	}

}
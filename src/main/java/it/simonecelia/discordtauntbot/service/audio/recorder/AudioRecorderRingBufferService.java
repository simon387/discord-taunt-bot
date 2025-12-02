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
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class AudioRecorderRingBufferService {

	private static final int SAMPLE_RATE = 48000;

	private static final int CHANNELS = 2; // Stereo

	private static final int BYTES_PER_SAMPLE = 2; // 16-bit = 2 bytes

	private static final int FRAME_SIZE = CHANNELS * BYTES_PER_SAMPLE; // 4 bytes per frame

	private static final int BYTES_PER_SECOND = SAMPLE_RATE * FRAME_SIZE;

	private static final int BUFFER_SIZE = BYTES_PER_SECOND * 3600 * 2; // 2 hour

	// Singleton instance
	private static volatile AudioRecorderRingBufferService instance;

	private static final Object LOCK = new Object ();

	private final String instanceId = UUID.randomUUID ().toString ().substring ( 0, 8 );

	private final byte[] ringBuffer = new byte[BUFFER_SIZE];

	private volatile int writePos = 0;

	private volatile boolean bufferFilledOnce = false;

	private final File output;

	private final ScheduledExecutorService scheduler;

	// Track last saved position to detect new data
	private volatile int lastSavedWritePos = 0;

	private volatile boolean lastSavedBufferFilledOnce = false;

	// Counters for debug - MUST be volatile for thread visibility
	private volatile long totalBytesWritten = 0;

	private volatile long lastLogTime = System.currentTimeMillis ();

	/**
	 * Gets or creates the singleton instance.
	 */
	public static AudioRecorderRingBufferService getInstance ( String outputName ) {
		if ( instance == null ) {
			synchronized ( LOCK ) {
				if ( instance == null ) {
					Log.info ( "[Recorder] Creating new singleton instance" );
					instance = new AudioRecorderRingBufferService ( outputName );
				}
			}
		}
		return instance;
	}

	/**
	 * Destroys the singleton instance and stops all background tasks.
	 */
	public static void destroyInstance () {
		synchronized ( LOCK ) {
			if ( instance != null ) {
				Log.info ( "[Recorder] Destroying singleton instance" );
				instance.shutdown ();
				instance = null;
			}
		}
	}

	/**
	 * Private constructor - use getInstance() instead.
	 */
	private AudioRecorderRingBufferService ( String outputName ) {
		this.output = new File ( outputName );

		// Ensure parent directory exists
		if ( output.getParentFile () != null && !output.getParentFile ().exists () ) {
			boolean mkdirsOutput = output.getParentFile ().mkdirs ();
			Log.infof ( "[Recorder:%s] Creating directory: %s", instanceId, mkdirsOutput );
		}

		this.scheduler = Executors.newSingleThreadScheduledExecutor ( r -> {
			Thread t = new Thread ( r );
			t.setName ( "AudioRecorder-" + instanceId );
			return t;
		} );

		// scheduleAtFixedRate(task, initialDelay, period, unit)
		// First execution after 5 minutes, then every 5 minutes
		scheduler.scheduleAtFixedRate ( this::saveLastHourToWav, 5, 5, TimeUnit.MINUTES );

		Log.infof ( "[Recorder:%s] *** NEW INSTANCE CREATED ***", instanceId );
		Log.infof ( "[Recorder:%s] Recording file name: %s", instanceId, output.getAbsolutePath () );
		Log.infof ( "[Recorder:%s] Buffer size: %d bytes (%.2f MB)", instanceId, BUFFER_SIZE, BUFFER_SIZE / 1024.0 / 1024.0 );
		Log.infof ( "[Recorder:%s] Expected format: %d Hz, %d channels, %d-bit", instanceId, SAMPLE_RATE, CHANNELS, BYTES_PER_SAMPLE * 8 );
		Log.infof ( "[Recorder:%s] Auto-save scheduled every 5 minutes (first save in 5 minutes)", instanceId );
	}

	/**
	 * Stops the scheduler and releases resources.
	 * Call this when the recorder is no longer needed.
	 */
	public void shutdown () {
		Log.infof ( "[Recorder:%s] *** SHUTTING DOWN ***", instanceId );
		scheduler.shutdown ();
		try {
			if ( !scheduler.awaitTermination ( 5, TimeUnit.SECONDS ) ) {
				scheduler.shutdownNow ();
			}
		} catch ( InterruptedException e ) {
			scheduler.shutdownNow ();
			Thread.currentThread ().interrupt ();
		}
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
		long now = System.currentTimeMillis ();
		if ( now - lastLogTime > 30000 ) { // Log every 30 seconds
			Log.infof ( "[Recorder:%s] Tot bytes received: %d (%.2f MB), chunk size: %d, writePos: %d, bufferFilled: %s",
							instanceId, totalBytesWritten, totalBytesWritten / 1024.0 / 1024.0, pcm.length, writePos, bufferFilledOnce );
			lastLogTime = now;
		}

		// Copy entire chunk
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
				Log.infof ( "[Recorder:%s] Ring buffer wrapped around", instanceId );
			}
		}
	}

	/**
	 * Saves the last hour of audio to WAV.
	 */
	public synchronized void saveLastHourToWav () {
		try {
			// Capture current state in local variables (thread-safe snapshot)
			int currentWritePos = writePos;
			boolean currentBufferFilled = bufferFilledOnce;
			long currentTotalBytes = totalBytesWritten;

			Log.infof ( "[Recorder:%s] Starting save operation to: %s", instanceId, output.getAbsolutePath () );
			Log.infof ( "[Recorder:%s] Current state - writePos: %d, bufferFilledOnce: %s, totalBytes: %d",
							instanceId, currentWritePos, currentBufferFilled, currentTotalBytes );
			Log.infof ( "[Recorder:%s] Last saved state - writePos: %d, bufferFilledOnce: %s",
							instanceId, lastSavedWritePos, lastSavedBufferFilledOnce );

			// Check if there's ANY data in the buffer
			if ( currentTotalBytes == 0 ) {
				Log.warnf ( "[Recorder:%s] No audio has been received yet (totalBytesWritten = 0)", instanceId );
				return;
			}

			// Check if there's new data since last save
			if ( currentWritePos == lastSavedWritePos && currentBufferFilled == lastSavedBufferFilledOnce ) {
				Log.warnf ( "[Recorder:%s] No new audio data since last save", instanceId );
				return;
			}

			byte[] audioData;
			int dataLength;

			if ( currentBufferFilled ) {
				// Buffer has wrapped around at least once - save full buffer
				audioData = new byte[BUFFER_SIZE];
				dataLength = BUFFER_SIZE;
				int endChunk = BUFFER_SIZE - currentWritePos;
				System.arraycopy ( ringBuffer, currentWritePos, audioData, 0, endChunk );
				System.arraycopy ( ringBuffer, 0, audioData, endChunk, currentWritePos );
				Log.infof ( "[Recorder] Buffer filled, saving full buffer (%d bytes)", dataLength );
			} else {
				// Buffer hasn't wrapped yet - save from start to writePos
				// Align to frame size to avoid partial frames
				dataLength = ( currentWritePos / FRAME_SIZE ) * FRAME_SIZE;
				if ( dataLength == 0 ) {
					Log.warn ( "[Recorder] writePos too small to form complete frames" );
					return;
				}
				audioData = new byte[dataLength];
				System.arraycopy ( ringBuffer, 0, audioData, 0, dataLength );
				Log.infof ( "[Recorder] Buffer not yet filled, saving %d bytes (%.2f%% of buffer)",
								dataLength, ( dataLength * 100.0 ) / BUFFER_SIZE );
			}

			Log.infof ( "[Recorder] Saving %d bytes (%.2f seconds) to WAV",
							dataLength, (double) dataLength / BYTES_PER_SECOND );

			writeWavFile ( audioData );

			// Update last saved position
			lastSavedWritePos = currentWritePos;
			lastSavedBufferFilledOnce = currentBufferFilled;

			// Verify file exists and has content
			if ( output.exists () ) {
				Log.infof ( "[Recorder] ✓ File saved successfully: %s (%.2f MB)",
								output.getAbsolutePath (), output.length () / 1024.0 / 1024.0 );
			} else {
				Log.errorf ( "[Recorder] ✗ File does not exist after write: %s", output.getAbsolutePath () );
			}

		} catch ( Exception e ) {
			Log.errorf ( e, "[Recorder] Error saving WAV to %s", output.getAbsolutePath () );
		}
	}

	/**
	 * Writes WAV file using atomic file replacement to avoid Linux file descriptor issues.
	 */
	private void writeWavFile ( byte[] audioData ) throws Exception {
		AudioFormat format = new AudioFormat (
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
			Files.move (
							tempFile.toPath (),
							output.toPath (),
							StandardCopyOption.REPLACE_EXISTING,
							StandardCopyOption.ATOMIC_MOVE
			);

			Log.infof ( "[Recorder] File moved to final destination: %d bytes", output.length () );

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
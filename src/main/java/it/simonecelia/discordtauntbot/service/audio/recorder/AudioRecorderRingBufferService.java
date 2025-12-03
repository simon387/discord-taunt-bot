package it.simonecelia.discordtauntbot.service.audio.recorder;

import io.quarkus.logging.Log;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
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

	// Buffer multipli: ogni chunk è 10 minuti
	private static final int CHUNK_DURATION_SECONDS = 600; // 10 minuti

	private static final int CHUNK_SIZE = BYTES_PER_SECOND * CHUNK_DURATION_SECONDS;

	private static final int NUM_CHUNKS = 24; // 24 × 10min = 4 ore

	// Singleton instance
	private static volatile AudioRecorderRingBufferService instance;

	private static final Object LOCK = new Object ();

	private final String instanceId = UUID.randomUUID ().toString ().substring ( 0, 8 );

	// Lista di buffer chunks invece di un singolo array gigante
	private final List<byte[]> ringBufferChunks = new ArrayList<> ();

	private volatile int currentChunkIndex = 0; // Chunk corrente in scrittura

	private volatile int writePos = 0; // Posizione nel chunk corrente

	private volatile boolean bufferFilledOnce = false;

	private final File output;

	private final ScheduledExecutorService scheduler;

	// Track last saved position
	private volatile int lastSavedChunkIndex = 0;

	private volatile int lastSavedWritePos = 0;

	private volatile boolean lastSavedBufferFilledOnce = false;

	// Counters for debug
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

		// Inizializza tutti i chunks
		for ( int i = 0; i < NUM_CHUNKS; i++ ) {
			ringBufferChunks.add ( new byte[CHUNK_SIZE] );
		}

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

		scheduler.scheduleAtFixedRate ( this::saveLastHourToWav, 5, 5, TimeUnit.MINUTES );

		long totalBufferSize = (long) CHUNK_SIZE * NUM_CHUNKS;
		Log.infof ( "[Recorder:%s] *** NEW INSTANCE CREATED ***", instanceId );
		Log.infof ( "[Recorder:%s] Recording file name: %s", instanceId, output.getAbsolutePath () );
		Log.infof ( "[Recorder:%s] Buffer: %d chunks × %.2f MB = %.2f MB total (%.1f hours)",
						instanceId, NUM_CHUNKS, CHUNK_SIZE / 1024.0 / 1024.0,
						totalBufferSize / 1024.0 / 1024.0,
						( totalBufferSize / (double) BYTES_PER_SECOND ) / 3600.0 );
		Log.infof ( "[Recorder:%s] Expected format: %d Hz, %d channels, %d-bit",
						instanceId, SAMPLE_RATE, CHANNELS, BYTES_PER_SAMPLE * 8 );
		Log.infof ( "[Recorder:%s] Auto-save scheduled every 5 minutes", instanceId );
	}

	/**
	 * Stops the scheduler and releases resources.
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
			Log.infof ( "[Recorder:%s] Tot bytes: %d (%.2f MB), chunk %d/%d, pos: %d, filled: %s",
							instanceId, totalBytesWritten, totalBytesWritten / 1024.0 / 1024.0,
							currentChunkIndex + 1, NUM_CHUNKS, writePos, bufferFilledOnce );
			lastLogTime = now;
		}

		int remaining = pcm.length;
		int offset = 0;

		while ( remaining > 0 ) {
			byte[] currentChunk = ringBufferChunks.get ( currentChunkIndex );
			int spaceInCurrentChunk = CHUNK_SIZE - writePos;
			int toCopy = Math.min ( remaining, spaceInCurrentChunk );

			System.arraycopy ( pcm, offset, currentChunk, writePos, toCopy );

			writePos += toCopy;
			offset += toCopy;
			remaining -= toCopy;

			// Se il chunk corrente è pieno, passa al prossimo
			if ( writePos >= CHUNK_SIZE ) {
				writePos = 0;
				currentChunkIndex++;

				if ( currentChunkIndex >= NUM_CHUNKS ) {
					currentChunkIndex = 0;
					bufferFilledOnce = true;
					Log.infof ( "[Recorder:%s] Ring buffer wrapped around (all chunks filled)", instanceId );
				}
			}
		}
	}

	/**
	 * Saves the last hours of audio to WAV.
	 */
	public synchronized void saveLastHourToWav () {
		try {
			// Capture current state
			int currentChunk = currentChunkIndex;
			int currentPos = writePos;
			boolean currentBufferFilled = bufferFilledOnce;
			long currentTotalBytes = totalBytesWritten;

			Log.infof ( "[Recorder:%s] Starting save operation to: %s", instanceId, output.getAbsolutePath () );
			Log.infof ( "[Recorder:%s] Current state - chunk: %d/%d, pos: %d, filled: %s, totalBytes: %d",
							instanceId, currentChunk + 1, NUM_CHUNKS, currentPos, currentBufferFilled, currentTotalBytes );

			// Check if there's ANY data
			if ( currentTotalBytes == 0 ) {
				Log.warnf ( "[Recorder:%s] No audio has been received yet", instanceId );
				return;
			}

			// Check if there's new data since last save
			if ( currentChunk == lastSavedChunkIndex &&
							currentPos == lastSavedWritePos &&
							currentBufferFilled == lastSavedBufferFilledOnce ) {
				Log.warnf ( "[Recorder:%s] No new audio data since last save", instanceId );
				return;
			}

			byte[] audioData = collectAudioData ( currentChunk, currentPos, currentBufferFilled );

			if ( audioData.length == 0 ) {
				Log.warn ( "[Recorder] No valid audio data to save" );
				return;
			}

			Log.infof ( "[Recorder] Saving %d bytes (%.2f seconds) to WAV",
							audioData.length, (double) audioData.length / BYTES_PER_SECOND );

			writeWavFile ( audioData );

			// Update last saved position
			lastSavedChunkIndex = currentChunk;
			lastSavedWritePos = currentPos;
			lastSavedBufferFilledOnce = currentBufferFilled;

			// Verify file exists
			if ( output.exists () ) {
				Log.infof ( "[Recorder] ✓ File saved: %s (%.2f MB)",
								output.getAbsolutePath (), output.length () / 1024.0 / 1024.0 );
			} else {
				Log.errorf ( "[Recorder] ✗ File does not exist after write: %s", output.getAbsolutePath () );
			}

		} catch ( Exception e ) {
			Log.errorf ( e, "[Recorder] Error saving WAV to %s", output.getAbsolutePath () );
		}
	}

	/**
	 * Raccoglie i dati audio dai chunks in ordine circolare.
	 */
	private byte[] collectAudioData ( int currentChunk, int currentPos, boolean bufferFilled ) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream ();

		if ( bufferFilled ) {
			// Il buffer ha fatto il giro completo - salva tutti i chunks in ordine
			// Partendo dal chunk successivo a quello corrente (il più vecchio)
			int oldestChunk = ( currentChunk + 1 ) % NUM_CHUNKS;

			// Copia i chunks completi dal più vecchio al chunk corrente
			for ( int i = 0; i < NUM_CHUNKS; i++ ) {
				int chunkIdx = ( oldestChunk + i ) % NUM_CHUNKS;
				byte[] chunk = ringBufferChunks.get ( chunkIdx );

				if ( chunkIdx == currentChunk ) {
					// Ultimo chunk: copia solo fino a writePos (allineato a frame)
					int alignedPos = ( currentPos / FRAME_SIZE ) * FRAME_SIZE;
					if ( alignedPos > 0 ) {
						baos.write ( chunk, 0, alignedPos );
					}
				} else {
					// Chunk completo
					baos.write ( chunk, 0, CHUNK_SIZE );
				}
			}

			Log.infof ( "[Recorder] Buffer filled, saving all chunks (oldest: %d)", oldestChunk + 1 );
		} else {
			// Il buffer non è ancora pieno - salva solo i chunks scritti
			for ( int i = 0; i <= currentChunk; i++ ) {
				byte[] chunk = ringBufferChunks.get ( i );

				if ( i == currentChunk ) {
					// Ultimo chunk parzialmente riempito
					int alignedPos = ( currentPos / FRAME_SIZE ) * FRAME_SIZE;
					if ( alignedPos > 0 ) {
						baos.write ( chunk, 0, alignedPos );
					}
				} else {
					// Chunk completo
					baos.write ( chunk, 0, CHUNK_SIZE );
				}
			}

			Log.infof ( "[Recorder] Buffer not filled, saving chunks 0-%d", currentChunk );
		}

		byte[] result = baos.toByteArray ();
		Log.infof ( "[Recorder] Collected %d bytes from chunks", result.length );
		return result;
	}

	/**
	 * Writes WAV file using atomic file replacement.
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

		var tempFile = new File ( output.getParentFile (), output.getName () + ".tmp" );

		try {
			long lengthInFrames = audioData.length / format.getFrameSize ();

			try ( var bais = new ByteArrayInputStream ( audioData );
							var ais = new AudioInputStream ( bais, format, lengthInFrames ) ) {
				AudioSystem.write ( ais, AudioFileFormat.Type.WAVE, tempFile );
			}

			if ( !tempFile.exists () || tempFile.length () == 0 ) {
				throw new IOException ( "Temporary file was not created or is empty: " + tempFile.getAbsolutePath () );
			}

			Log.infof ( "[Recorder] Temp file written: %d bytes", tempFile.length () );

			Files.move (
							tempFile.toPath (),
							output.toPath (),
							StandardCopyOption.REPLACE_EXISTING,
							StandardCopyOption.ATOMIC_MOVE
			);

			Log.infof ( "[Recorder] File moved to final destination: %d bytes", output.length () );

		} finally {
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
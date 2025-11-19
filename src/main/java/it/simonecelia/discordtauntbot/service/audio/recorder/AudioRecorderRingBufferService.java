package it.simonecelia.discordtauntbot.service.audio.recorder;

import io.quarkus.logging.Log;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	// Contatori per debug
	private long totalBytesWritten = 0;
	private long lastLogTime = System.currentTimeMillis();

	public AudioRecorderRingBufferService(String outputName) {
		this.output = new File(outputName);
		scheduler.scheduleAtFixedRate(this::saveLastHourToWav, 5, 5, TimeUnit.MINUTES);
		Log.infof("[Recorder] Recording file name: %s", output.getName());
		Log.infof("[Recorder] Buffer size: %d bytes (%.2f MB)", BUFFER_SIZE, BUFFER_SIZE / 1024.0 / 1024.0);
		Log.infof("[Recorder] Expected format: %d Hz, %d channels, %d-bit", SAMPLE_RATE, CHANNELS, BYTES_PER_SAMPLE * 8);
	}

	/**
	 * Scrive i chunk interi nel ring buffer
	 */
	public synchronized void writeToRingBuffer(byte[] pcm) {
		if (pcm == null || pcm.length == 0) {
			return;
		}

		// Log periodico per debug
		totalBytesWritten += pcm.length;
		long now = System.currentTimeMillis();
		if (now - lastLogTime > 30000) { // Log ogni 30 secondi
			Log.infof("[Recorder] Total bytes received: %d (%.2f MB), chunk size: %d",
							totalBytesWritten, totalBytesWritten / 1024.0 / 1024.0, pcm.length);
			lastLogTime = now;
		}

		// Copia l'intero chunk
		int remaining = pcm.length;
		int offset = 0;

		while (remaining > 0) {
			int spaceToEnd = ringBuffer.length - writePos;
			int toCopy = Math.min(remaining, spaceToEnd);

			System.arraycopy(pcm, offset, ringBuffer, writePos, toCopy);

			writePos += toCopy;
			offset += toCopy;
			remaining -= toCopy;

			if (writePos >= ringBuffer.length) {
				writePos = 0;
				bufferFilledOnce = true;
				Log.info("[Recorder] Ring buffer wrapped around");
			}
		}
	}

	/**
	 * Salva l'ultima ora di audio in WAV
	 */
	public synchronized void saveLastHourToWav() {
		try {
			if (writePos == 0 && !bufferFilledOnce) {
				Log.warn("[Recorder] No audio data to save yet");
				return;
			}

			byte[] audioData;
			int dataLength;

			if (bufferFilledOnce) {
				audioData = new byte[BUFFER_SIZE];
				dataLength = BUFFER_SIZE;
				int endChunk = BUFFER_SIZE - writePos;
				System.arraycopy(ringBuffer, writePos, audioData, 0, endChunk);
				System.arraycopy(ringBuffer, 0, audioData, endChunk, writePos);
			} else {
				// Allinea alla dimensione del frame per evitare frame parziali
				dataLength = (writePos / FRAME_SIZE) * FRAME_SIZE;
				audioData = new byte[dataLength];
				System.arraycopy(ringBuffer, 0, audioData, 0, dataLength);
			}

			Log.infof("[Recorder] Saving %d bytes (%.2f seconds) to WAV",
							dataLength, (double) dataLength / BYTES_PER_SECOND);

			// FIX: Converti da little-endian a big-endian per Java Sound API
			byte[] convertedData = convertLittleEndianToBigEndian(audioData);

			writeWavFile(convertedData);
			Log.infof("[Recorder] Saved to: %s (%.2f MB)",
							output.getAbsolutePath(), output.length() / 1024.0 / 1024.0);
		} catch (Exception e) {
			Log.error("[Recorder] Error saving WAV", e);
		}
	}

	/**
	 * Converte l'audio da little-endian (JDA) a big-endian (Java Sound)
	 */
	private byte[] convertLittleEndianToBigEndian(byte[] littleEndianData) {
		byte[] bigEndianData = new byte[littleEndianData.length];

		// Converti ogni sample 16-bit
		for (int i = 0; i < littleEndianData.length; i += 2) {
			// Little-endian: byte basso prima, byte alto dopo
			// Big-endian: byte alto prima, byte basso dopo
			bigEndianData[i] = littleEndianData[i + 1];     // byte alto
			bigEndianData[i + 1] = littleEndianData[i];     // byte basso
		}

		Log.info("[Recorder] Converted audio from little-endian to big-endian");
		return bigEndianData;
	}

	private void writeWavFile(byte[] audioData) throws Exception {
		// IMPORTANTE: Ora usiamo BIG-ENDIAN perché abbiamo convertito i dati
		var format = new AudioFormat(
						AudioFormat.Encoding.PCM_SIGNED,  // Encoding
						SAMPLE_RATE,                       // Sample rate
						16,                                // Bits per sample
						CHANNELS,                          // Channels
						FRAME_SIZE,                        // Frame size
						SAMPLE_RATE,                       // Frame rate
						true                               // BIG-ENDIAN (true = big-endian)
		);

		Log.infof("[Recorder] Audio format: %s", format);

		long lengthInFrames = audioData.length / format.getFrameSize();

		try (var bais = new ByteArrayInputStream(audioData);
						var ais = new AudioInputStream(bais, format, lengthInFrames)) {
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, output);
		}
	}

	public void shutdown() {
		scheduler.shutdown();
		try {
			// Salva prima di chiudere
			saveLastHourToWav();

			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
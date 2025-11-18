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
	private static final int CHANNELS = 2; // Stereo - JDA fornisce audio stereo
	private static final int BYTES_PER_SAMPLE = 2;
	private static final int BYTES_PER_SECOND = SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE;
	private static final int BUFFER_SIZE = BYTES_PER_SECOND * 3600; // 1 ora

	private final byte[] ringBuffer = new byte[BUFFER_SIZE];
	private int writePos = 0;
	private boolean bufferFilledOnce = false;
	private final File output;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	public AudioRecorderRingBufferService(String outputName) {
		this.output = new File(outputName);
		scheduler.scheduleAtFixedRate(this::saveLastHourToWav, 5, 5, TimeUnit.MINUTES);
		Log.infof("[Recorder] Recording file name: %s", output.getName());
	}

	/**
	 * Scrive i byte PCM ricevuti da JDA nel buffer circolare
	 */
	public synchronized void writeToRingBuffer(byte[] pcm) {
		for (byte b : pcm) {
			ringBuffer[writePos++] = b;
			if (writePos >= ringBuffer.length) {
				writePos = 0;
				bufferFilledOnce = true;
			}
		}
	}

	/**
	 * Salva l'ultima ora di audio in WAV
	 */
	public synchronized void saveLastHourToWav() {
		try {
			byte[] audioData;

			if (bufferFilledOnce) {
				audioData = new byte[BUFFER_SIZE];
				int endChunk = BUFFER_SIZE - writePos;
				System.arraycopy(ringBuffer, writePos, audioData, 0, endChunk);
				System.arraycopy(ringBuffer, 0, audioData, endChunk, writePos);
			} else {
				audioData = new byte[writePos];
				System.arraycopy(ringBuffer, 0, audioData, 0, writePos);
			}

			writeWavFile(audioData);
			Log.info("[Recorder] Saved last hour to WAV");
		} catch (Exception e) {
			Log.error("[Recorder] Error saving WAV", e);
		}
	}

	private void writeWavFile(byte[] audioData) throws Exception {
		// AudioFormat: 48 kHz, 16-bit, stereo, signed, LITTLE-ENDIAN
		// JDA fornisce audio in little-endian, quindi l'ultimo parametro deve essere false
		var format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);

		try (var bais = new ByteArrayInputStream(audioData);
						var ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize())) {
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, output);
		}
	}

	public void shutdown() {
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
package it.simonecelia.discordtauntbot.service.audio.recorder;

import io.quarkus.logging.Log;
import net.dv8tion.jda.api.audio.UserAudio;

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

	private static final int CHANNELS = 1; // Mono per utente

	private static final int BYTES_PER_SAMPLE = 2;

	private static final int BYTES_PER_SECOND = SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE;

	private static final int BUFFER_SIZE = BYTES_PER_SECOND * 3600; // 1 ora

	private final byte[] ringBuffer = new byte[BUFFER_SIZE];

	private int writePos = 0;

	private boolean bufferFilledOnce = false;

	private final File output;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor ();

	public AudioRecorderRingBufferService ( String outputName ) {
		this.output = new File ( outputName );
		scheduler.scheduleAtFixedRate ( this::saveLastHourToWav, 1, 1, TimeUnit.MINUTES );//TODO 5,5
	}

	public synchronized void writeToRingBuffer ( byte[] floatBytes ) {
//		for ( byte b : pcm ) {
//			ringBuffer[writePos++] = b;
//			if ( writePos >= ringBuffer.length ) {
//				writePos = 0;
//				bufferFilledOnce = true;
//			}
//		}
		for (int i = 0; i < floatBytes.length; i += 4) { // JDA usa float32 per sample
			int bits = ((floatBytes[i+3] & 0xFF) << 24) | ((floatBytes[i+2] & 0xFF) << 16)
							| ((floatBytes[i+1] & 0xFF) << 8) | (floatBytes[i] & 0xFF);
			float f = Float.intBitsToFloat(bits);
			f = Math.max(-1.0f, Math.min(1.0f, f)); // clamp
			short pcm = (short)(f * Short.MAX_VALUE);
			ringBuffer[writePos++] = (byte)(pcm & 0xFF);
			ringBuffer[writePos++] = (byte)((pcm >> 8) & 0xFF);

			if (writePos >= ringBuffer.length) {
				writePos = 0;
				bufferFilledOnce = true;
			}
		}
	}

	public synchronized void handleUserAudio ( UserAudio userAudio ) {
		writeToRingBuffer ( userAudio.getAudioData ( 1.0f ) );
	}

	public synchronized void saveLastHourToWav () {
		try {
			byte[] audioData;

			if ( bufferFilledOnce ) {
				audioData = new byte[BUFFER_SIZE];
				int endChunk = BUFFER_SIZE - writePos;
				System.arraycopy ( ringBuffer, writePos, audioData, 0, endChunk );
				System.arraycopy ( ringBuffer, 0, audioData, endChunk, writePos );
			} else {
				audioData = new byte[writePos];
				System.arraycopy ( ringBuffer, 0, audioData, 0, writePos );
			}

			writeWavFile ( audioData );
		} catch ( Exception e ) {
			Log.error ( e );
		}
	}

	private void writeWavFile ( byte[] audioData ) throws Exception {
		var format = new AudioFormat ( SAMPLE_RATE, 16, CHANNELS, true, false );
		try ( var bais = new ByteArrayInputStream ( audioData );
						var ais = new AudioInputStream ( bais, format, audioData.length / format.getFrameSize () ) ) {
			AudioSystem.write ( ais, AudioFileFormat.Type.WAVE, output );
		}
	}
}

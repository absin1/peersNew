package net.sourceforge.peers.demo;

/*
This file is part of Peers, a java SIP softphone.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Copyright 2010, 2011, 2012 Yohann Martineau 
*/

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio.Builder;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.media.AbstractSoundManager;
import net.sourceforge.peers.sip.Utils;

public class StreamSoundManager3 extends AbstractSoundManager {
	public final static int BUFFER_SIZE = 256;

	private AudioFormat audioFormat;
	private TargetDataLine targetDataLine;
	private SourceDataLine sourceDataLine;
	private Object sourceDataLineMutex;
	private DataLine.Info targetInfo;
	private DataLine.Info sourceInfo;
	private FileOutputStream microphoneOutput;
	private FileOutputStream speakerInput;
	private boolean mediaDebug;
	private Logger logger;
	private String peersHome;
	private SpeechClient speechClient;
	private RecognitionConfig recConfig;
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	private int accumulationCounter = 0;
	private int maxAccumulation = 50; // 3 seconds
	int counter = 0;

	public StreamSoundManager3(boolean mediaDebug, Logger logger, String peersHome) {
		try {
			speechClient = SpeechClient.create();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.mediaDebug = mediaDebug;
		this.logger = logger;
		this.peersHome = peersHome;
		if (peersHome == null) {
			this.peersHome = Utils.DEFAULT_PEERS_HOME;
		}
		// linear PCM 8kHz, 16 bits signed, mono-channel, little endian
		audioFormat = new AudioFormat(8000, 16, 1, true, false);
		targetInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
		sourceInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		sourceDataLineMutex = new Object();
	}

	@Override
	public void init() {
		recConfig = RecognitionConfig.newBuilder()
				.setEncoding(com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding.LINEAR16)
				.setLanguageCode("en-IN").setSampleRateHertz(8000).build();

		logger.debug("openAndStartLines");
		if (mediaDebug) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			String date = simpleDateFormat.format(new Date());
			StringBuffer buf = new StringBuffer();
			buf.append(peersHome).append(File.separator);
			buf.append(MEDIA_DIR).append(File.separator);
			buf.append(date).append("_");
			buf.append(audioFormat.getEncoding()).append("_");
			buf.append(audioFormat.getSampleRate()).append("_");
			buf.append(audioFormat.getSampleSizeInBits()).append("_");
			buf.append(audioFormat.getChannels()).append("_");
			buf.append(audioFormat.isBigEndian() ? "be" : "le");
			try {
				microphoneOutput = new FileOutputStream(buf.toString() + "_microphone.output");
				speakerInput = new FileOutputStream(buf.toString() + "_speaker.input");
			} catch (FileNotFoundException e) {
				logger.error("cannot create file", e);
				return;
			}
		}
		// AccessController.doPrivileged added for plugin compatibility
		AccessController.doPrivileged(new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				try {
					targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
					targetDataLine.open(audioFormat);
				} catch (LineUnavailableException e) {
					logger.error("target line unavailable", e);
					return null;
				} catch (SecurityException e) {
					logger.error("security exception", e);
					return null;
				} catch (Throwable t) {
					logger.error("throwable " + t.getMessage());
					return null;
				}
				targetDataLine.start();
				synchronized (sourceDataLineMutex) {
					try {
						sourceDataLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
						sourceDataLine.open(audioFormat);
					} catch (LineUnavailableException e) {
						logger.error("source line unavailable", e);
						return null;
					}
					sourceDataLine.start();
				}
				return null;
			}
		});

	}

	@Override
	public synchronized void close() {
		logger.debug("closeLines");
		if (microphoneOutput != null) {
			try {
				microphoneOutput.close();
			} catch (IOException e) {
				logger.error("cannot close file", e);
			}
			microphoneOutput = null;
		}
		if (speakerInput != null) {
			try {
				speakerInput.close();
			} catch (IOException e) {
				logger.error("cannot close file", e);
			}
			speakerInput = null;
		}
		// AccessController.doPrivileged added for plugin compatibility
		AccessController.doPrivileged(new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				if (targetDataLine != null) {
					targetDataLine.close();
					targetDataLine = null;
				}
				synchronized (sourceDataLineMutex) {
					if (sourceDataLine != null) {
						sourceDataLine.drain();
						sourceDataLine.stop();
						sourceDataLine.close();
						sourceDataLine = null;
					}
				}
				return null;
			}
		});
	}

	@Override
	public synchronized byte[] readData() {
		// TODO Not sending any audio right now
		return null;
	}

	@Override
	public int writeData(byte[] buffer, int offset, int length) {
		if (accumulationCounter++ < maxAccumulation)
			try {
				outputStream.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		else {
			accumulationCounter = 0;
			counter++;

			Runnable myRunnable = new Runnable() {
				public void run() {
					byte[] byteArray = outputStream.toByteArray();
					if (counter > 10) {
						counter = 0;
						outputStream = new ByteArrayOutputStream();
					}
					ByteString audioBytes = ByteString.copyFrom(byteArray);
					Builder newBuilder = RecognitionAudio.newBuilder();
					RecognitionAudio audio = newBuilder.setContent(audioBytes).build();
					RecognizeResponse response = speechClient.recognize(recConfig, audio);
					List<SpeechRecognitionResult> results = response.getResultsList();
					for (SpeechRecognitionResult result : results) {
						SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
						System.err.printf("<<<<<<<<<<<<Transcription: %s>>>>>>>>>>>>%n", alternative.getTranscript());
					}
				}
			};
			Thread thread = new Thread(myRunnable);
			thread.start();
		}
		return buffer.length;
	}

}

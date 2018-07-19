package net.sourceforge.peers.demo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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


import java.io.File;
import java.io.FileInputStream;
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

import net.sourceforge.peers.Logger;
import net.sourceforge.peers.media.AbstractSoundManager;
import net.sourceforge.peers.sip.Utils;
import talentify.ai.mitsuku.BotSingleton;
import talentify.ai.mitsuku.Chat;

import org.apache.commons.io.IOUtils;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
public class StreamSoundManager extends AbstractSoundManager {
	public final static int BUFFER_SIZE = 256;

	private Chat chatSession = BotSingleton.getInstance().getChatS();
	private FileInputStream fileInputStream;
	private ByteArrayInputStream bis ;
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
	private byte[] byteArray;
	private int counter = 0;
	private TextToSpeechClient textToSpeechClient;
	private SpeechClient speechClient;
	private boolean isSpeakingRequired = false;
	private boolean isSpeechReady =false;
	private int accumulationCounter = 0;
	private int maxAccumulation = 200;
	private ByteArrayOutputStream accumulatedStream  = new ByteArrayOutputStream();
	private String text1 = "Hello WORLD HOW ARE YOU DOING TODAY. THIS IS an automated bot called Zoya at your service";
	private String text;
public StreamSoundManager(boolean mediaDebug, Logger logger, String peersHome) {
	try {
		textToSpeechClient = TextToSpeechClient.create();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	try {
		speechClient = SpeechClient.create();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	getGoogleSpeech(text1);
	
	
	/*try {
		fileInputStream = new FileInputStream("/home/absin/Downloads/output1.wav");
		byteArray = IOUtils.toByteArray(fileInputStream);
		bis = new ByteArrayInputStream(byteArray);
	 } catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}*/
	
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

private void getGoogleSpeech(String textss) {
	if(textss != null)
		text =textss;
	System.err.println("STARRRRTTTTEDDDDDD");
 
	SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

	// Build the voice request
	VoiceSelectionParams voice = VoiceSelectionParams.newBuilder().setLanguageCode("en-IN").setSsmlGenderValue(2).build();

	// Select the type of audio file you want returned
	AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).setSampleRateHertz(8000) // MP3 audio.
			.build();

	// Perform the text-to-speech request
	SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

	// Get the audio contents from the response
	ByteString audioContents = response.getAudioContent();

	// Write the response to the output file.
	byteArray = audioContents.toByteArray();
	

	bis = new ByteArrayInputStream(byteArray);
	isSpeechReady = true;
	System.err.println("DDDDOOOOOONNNNNEEEEEEEEEEEEEEE");
}

@Override
public void init() {
	getGoogleSpeech(text1);
	logger.debug("openAndStartLines");
    if (mediaDebug) {
        SimpleDateFormat simpleDateFormat =
            new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
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
            microphoneOutput = new FileOutputStream(buf.toString()
                    + "_microphone.output");
            speakerInput = new FileOutputStream(buf.toString()
                    + "_speaker.input");
        } catch (FileNotFoundException e) {
            logger.error("cannot create file", e);
            return;
        }
    }
    // AccessController.doPrivileged added for plugin compatibility
    AccessController.doPrivileged(
        new PrivilegedAction<Void>() {

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
    /*if (targetDataLine == null) {
        return null;
    }
    int ready = targetDataLine.available();
    while (ready == 0) {
        try {
            Thread.sleep(2);
            ready = targetDataLine.available();
        } catch (InterruptedException e) {
            return null;
        }
    }
    if (ready <= 0) {
        return null;
    }
    byte[] buffer = new byte[ready];
    targetDataLine.read(buffer, 0, buffer.length);
    if (mediaDebug) {
        try {
            microphoneOutput.write(buffer, 0, buffer.length);
        } catch (IOException e) {
            logger.error("cannot write to file", e);
            return null;
        }
    }
    return buffer;*/
	
	
	
	
	/*if (fileInputStream == null) {
        return null;
    }
	
	byte buffer[] = new byte[BUFFER_SIZE];
	
	try {
		if (fileInputStream.read(buffer) >= 0) {
			Thread.sleep(15);
			return buffer;
		} else {
			fileInputStream.close();
			fileInputStream = null;
		}
	} catch (IOException | InterruptedException e) {
		e.printStackTrace();
	}
	return null;
		 */

		// System.err.println("isSpeakingRequired>>"+isSpeakingRequired);
		byte buffer[] = new byte[BUFFER_SIZE];
		if (bis == null) {
			return buffer;
		}

		try {
			if (bis.read(buffer) >= 0) {
				Thread.sleep(15);
				return buffer;
			} else {
				bis.close();
				bis = null;
				isSpeechReady = false;
				return buffer;
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return buffer;

		/*
		 * if (bis == null) { return null; }
		 * 
		 * byte buffer[] = new byte[BUFFER_SIZE];
		 * 
		 * try { if (bis.read(buffer) >= 0) { Thread.sleep(15); return buffer; } else {
		 * bis.close(); bis = null; isSpeakingRequired = false; } } catch (IOException |
		 * InterruptedException e) { e.printStackTrace(); } return null;
		 */

		/*
		 * int length = 256; byte[] buffer = new byte[length]; int j = 0; while (counter
		 * < byteArray.length && j < length) { buffer[j++] = byteArray[counter++]; }
		 * System.out.println("Counter>>" + counter); return buffer;
		 */

	}

	@Override
	public int writeData(byte[] buffer, int offset, int length) {
		/*
		 * int numberOfBytesWritten; synchronized (sourceDataLineMutex) { if
		 * (sourceDataLine == null) { return 0; } numberOfBytesWritten =
		 * sourceDataLine.write(buffer, offset, length); } if (mediaDebug) { try {
		 * speakerInput.write(buffer, offset, numberOfBytesWritten); } catch
		 * (IOException e) { logger.error("cannot write to file", e); return -1; } }
		 * return numberOfBytesWritten;
		 */
		if (++accumulationCounter < maxAccumulation) {
			try {
				// System.err.println(accumulationCounter+">>>>>>");
				accumulatedStream.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// System.err.println(">>>Inititaling speech to text>>");
			initiateGoogleSpeechToText();
		}
		int length2 = buffer.length;
		return length2;
	}

	private void initiateGoogleSpeechToText() {
		 System.err.println("Starting speech to text  >>>>>>"+accumulatedStream.size());
		ByteString audioBytes = ByteString.copyFrom(accumulatedStream.toByteArray());
		RecognitionConfig config = RecognitionConfig.newBuilder()
				.setEncoding(com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding.LINEAR16)
				.setSampleRateHertz(8000).setLanguageCode("en-IN").build();
		RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

		// Performs speech recognition on the audio file
		RecognizeResponse response = speechClient.recognize(config, audio);
		List<SpeechRecognitionResult> results = response.getResultsList();

		for (SpeechRecognitionResult result : results) {
			// There can be several alternative transcripts for a given chunk of speech.
			// Just use the
			// first (most likely) one here.
			SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
			System.err.printf("Transcription: %s%n", alternative.getTranscript());
			text = alternative.getTranscript();
			text = getMitsukuResponse(text);
			getGoogleSpeech(null);

		}
		accumulationCounter = 0;
		accumulatedStream.reset();
		// System.err.println("Finished speech to text >>>>>>");

	}
	private String getMitsukuResponse(String query) {
		if (talentify.ai.mitsuku.MagicBooleans.trace_mode)
			System.out.println("STATE=" + query + ":THAT=" + chatSession.thatHistory.get(0).get(0) + ":TOPIC="
					+ chatSession.predicates.get("topic"));
		String speech = chatSession.multisentenceRespond(query);
		while (speech.contains("&lt;"))
			speech = speech.replace("&lt;", "<");
		while (speech.contains("&gt;"))
			speech = speech.replace("&gt;", ">");
		return speech;
	}
}

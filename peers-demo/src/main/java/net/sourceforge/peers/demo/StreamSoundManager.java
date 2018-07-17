package net.sourceforge.peers.demo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

import net.sourceforge.peers.media.AbstractSoundManager;

public class StreamSoundManager extends AbstractSoundManager {
	public final static int BUFFER_SIZE = 8192;
	private TextToSpeechClient textToSpeechClient;
	private String text = "Hello WORLD HOW ARE YOU DOING TODAY. THIS IS CHAMPA KA BALAATKAAR";
	private byte[] byteArray;
	private int counter = 0;
	FileInputStream fileInputStream = null;

	@Override
	public synchronized byte[] readData() {
		int length = 8192;
		byte[] buffer = new byte[length];
		int j = 0;
		while (counter < byteArray.length && j < length) {
			buffer[j++] = byteArray[counter++];
		}
		System.out.println("Counter>>" + counter);
		return buffer;
		/*byte buffer[] = new byte[BUFFER_SIZE];
		
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
		}*/
		//return null;
	}

	@Override
	public void init() {
		counter = 0;
		try {
			fileInputStream = new FileInputStream(new File("C:\\Users\\absin\\Downloads\\output1.wav"));
			byteArray = IOUtils.readFully(fileInputStream, 73728) ;//toByteArray(fileInputStream);
		} catch (Exception e) {
			// TODO: handle exception
		}
		/*-try {
			textToSpeechClient = TextToSpeechClient.create();
			SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
		
			// Build the voice request
			VoiceSelectionParams voice = VoiceSelectionParams.newBuilder().setLanguageCode("en-IN").build();
		
			// Select the type of audio file you want returned
			AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).setSampleRateHertz(8000) // MP3 audio.
					.build();
		
			// Perform the text-to-speech request
			SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
		
			// Get the audio contents from the response
			ByteString audioContents = response.getAudioContent();
		
			// Write the response to the output file.
			byteArray = audioContents.toByteArray();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public int writeData(byte[] buffer, int offset, int length) {
		// TODO Auto-generated method stub
		return 0;
	}

}

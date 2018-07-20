package net.sourceforge.peers.demo;

//Imports the Google Cloud client library
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Google Cloud TextToSpeech API sample application. Example usage: mvn package
 * exec:java -Dexec.mainClass='com.example.texttospeech.SynthesizeText'
 * -Dexec.args='text "hello"'
 */
public class TextToSpeech {
	public static void main(String[] args) {
		String text = "On a dark desert highway, cool wind in my hair.Warm smell of colitas, rising up through the air.Up ahead in the distance, I saw a shimmering light.My head grew heavy and my sight grew dim.I had to stop for the night..There she stood in the doorway;.I heard the mission bell.And I was thinking to myself.'This could be heaven or this could be Hell'.Then she lit up a candle and she showed me the way.There were voices down the corridor,.I thought I heard them say.Welcome to the Hotel California.Such a lovely place (such a lovely place).Such a lovely face..Plenty of room at the Hotel California.Any time of year (any time of year) you can find it here.Her mind is Tiffany-twisted, she got the Mercedes bends.";
		try {
			synthesizeText(text);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void synthesizeText(String text) throws Exception {
		// Instantiates a client
		try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
			// Set the text input to be synthesized
			SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

			// Build the voice request
			VoiceSelectionParams voice = VoiceSelectionParams.newBuilder().setLanguageCode("en-US") // languageCode =
																									// "en_us"
					.setSsmlGender(SsmlVoiceGender.FEMALE) // ssmlVoiceGender = SsmlVoiceGender.FEMALE
					.build();

			// Select the type of audio file you want returned
			AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).setSampleRateHertz(8000) // MP3 audio.
					.build();

			// Perform the text-to-speech request
			SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

			// Get the audio contents from the response
			ByteString audioContents = response.getAudioContent();

			// Write the response to the output file.
			try (OutputStream out = new FileOutputStream("/home/absin/Downloads/output2.wav")) {
				out.write(audioContents.toByteArray());
				System.out.println("Audio content written to file \"output.mp3\"");
			}
		}
	}
}

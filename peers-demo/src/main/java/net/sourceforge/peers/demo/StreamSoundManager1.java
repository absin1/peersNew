package net.sourceforge.peers.demo;

import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognitionMetadata;
import com.google.cloud.speech.v1p1beta1.RecognitionMetadata.InteractionType;
import com.google.cloud.speech.v1p1beta1.RecognitionMetadata.MicrophoneDistance;
import com.google.cloud.speech.v1p1beta1.RecognitionMetadata.RecordingDeviceType;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionResult;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse;
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
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.sourceforge.peers.Logger;
import net.sourceforge.peers.media.AbstractSoundManager;
import net.sourceforge.peers.sip.Utils;
import talentify.ai.mitsuku.BotSingleton;
import talentify.ai.mitsuku.Chat;
import talentify.ai.nlp.CosineDifferenceServices;
import talentify.sales.simulation.Edge;
import talentify.sales.simulation.Experience;
import talentify.sales.simulation.Node;

import org.apache.commons.io.IOUtils;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.ByteString;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;

public class StreamSoundManager1 extends AbstractSoundManager {
	public final static int BUFFER_SIZE = 256;

	private Chat chatSession = BotSingleton.getInstance().getChatS();
	private FileInputStream fileInputStream;
	private ByteArrayInputStream bis;
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
	private boolean isSpeechReady = false;
	private int accumulationCounter = 0;
	private int maxAccumulation = 100;
	private ByteArrayOutputStream accumulatedStream = new ByteArrayOutputStream();
	private String text1 = "Hello"/*
									 * "Hello WORLD HOW ARE YOU DOING TODAY. THIS IS an automated bot called Zoya at your service"
									 */;
	private String text;
	private RecognitionConfig recConfig;
	private StreamingRecognitionConfig streamConfig;
	private ResponseObserver<StreamingRecognizeResponse> responseObserver;
	private Boolean responseIdentificationStarted = false;
	private StreamingRecognizeRequest streamingRecognizeRequest;
	private ClientStream<StreamingRecognizeRequest> clientStream;
	private Boolean isPrimed = true;

	public StreamSoundManager1(boolean mediaDebug, Logger logger, String peersHome) {
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
		// getGoogleSpeech(text1);
		prepareIntentUtteranceDataFromDialogflowDump();
		try {
			experience = getSampleExperience();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
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

	private void getGoogleSpeech(String textss) {
		if (textss != null)
			text = textss;
		System.err.println("STARRRRTTTTEDDDDDD");

		SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

		// Build the voice request
		VoiceSelectionParams voice = VoiceSelectionParams.newBuilder().setLanguageCode("en-IN").setSsmlGenderValue(2)
				.build();

		// Select the type of audio file you want returned
		AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16)
				.setSampleRateHertz(8000) // MP3 audio.
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

		responseObserver = new ResponseObserver<StreamingRecognizeResponse>() {

			public void onStart(StreamController controller) {
				// do nothing
				System.err.println("\n\n\nStarted \n\n" + System.currentTimeMillis());
			}

			public void onResponse(StreamingRecognizeResponse response) {
				System.err.println("REAL Time Response >>" + response);
			}

			public void onComplete() {
				System.err.println("\n\n\nCompleted\n\n");
			}

			public void onError(Throwable t) {
				System.err.println(t);
			}
		};
		clientStream = speechClient.streamingRecognizeCallable().splitCall(responseObserver);
		recConfig = RecognitionConfig.newBuilder()

				.setLanguageCode("en-IN").setSampleRateHertz(8000).build();

		streamConfig = StreamingRecognitionConfig.newBuilder().setConfig(recConfig).build();
		ResponseApiStreamingObserver<StreamingRecognizeResponse> responseObserver = new ResponseApiStreamingObserver<>();

		BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable = speechClient
				.streamingRecognizeCallable();

		ApiStreamObserver<StreamingRecognizeRequest> requestObserver = callable.bidiStreamingCall(responseObserver);

		// The first request must **only** contain the audio configuration:
		requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamConfig).build());

		// Subsequent requests must **only** contain the audio data.
		try {
			requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
					.setAudioContent(ByteString.copyFrom(
							IOUtils.toByteArray(new FileInputStream(new File("/home/absin/Downloads/output1.wav")))))
					.build());

			// Mark transmission as completed after sending the data.
			requestObserver.onCompleted();

			List<StreamingRecognizeResponse> responses = responseObserver.future().get();

			for (StreamingRecognizeResponse response : responses) {
				// For streaming recognize, the results list has one is_final result (if
				// available) followed
				// by a number of in-progress results (if iterim_results is true) for subsequent
				// utterances.
				// Just print the first result here.
				StreamingRecognitionResult result = response.getResultsList().get(0);
				// There can be several alternative transcripts for a given chunk of speech.
				// Just use the
				// first (most likely) one here.
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
				System.out.printf("Transcript : %s\n", alternative.getTranscript());
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	}

	@Override
	public int writeData(byte[] buffer, int offset, int length) {

		// System.err.println(Arrays.toString(buffer));

		/*-streamingRecognizeRequest = StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(buffer))
				.build();
		clientStream.send(streamingRecognizeRequest);
		*/
		return buffer.length;
	}

	private String[] resetSpeech = new String[] { "hmmm", "sorry", "can you repeat yourself" };
	private Experience experience;
	private HashMap<String, HashSet<String>> intentUtterances = new HashMap<>();
	private JsonParser jsonParser = new JsonParser();
	private CosineDifferenceServices cosineDifferenceServices = new CosineDifferenceServices();

	private String getSpeech(String query) {
		String speech;
		speech = getWorkflowResponse(query);
		if (speech == null) {
			if (query != null && query.toLowerCase().contains("zoya")) {
				try {
					query = query.replaceAll("zoya", " ").replaceAll("Zoya", " ");
					speech = getMitsukuResponse(query);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				logger.debug("No intent was met, and no Zoya was present, so not sending anything!");
				speech = resetSpeech[new Random().nextInt(resetSpeech.length)];
			}
		}

		return speech;
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

	private String getWorkflowResponse(String query) {
		try {
			if (query.length() == 0 || query == null)
				return null;
			Node newNode = identifyNodeLocal(query);
			if (newNode == null)
				return null;
			Edge edge = getWeightedRandomEdgeSpeech(newNode);
			return edge.getContent();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	private Node identifyNodeLocal(String query) {
		HashMap<String, Double> intentMaxScore = new HashMap<>();
		String intentName = null;
		Double maxIntentScore = Double.MIN_VALUE;
		for (String intentNamee : intentUtterances.keySet()) {
			HashSet<String> individualIntentUtterances = intentUtterances.get(intentNamee);
			for (String individualIntentUtterance : individualIntentUtterances) {
				double cosineScore = performCosineSimilarity(individualIntentUtterance, query);
				if (!intentMaxScore.containsKey(intentNamee))
					intentMaxScore.put(intentNamee, cosineScore);
				else if (intentMaxScore.get(intentNamee) < cosineScore)
					intentMaxScore.put(intentNamee, cosineScore);
			}
		}
		if (intentMaxScore.keySet().size() == intentUtterances.keySet().size())
			logger.error("NOTHING WRONG HAD HAPPENED SO FAR");
		for (String intentNamee : intentMaxScore.keySet()) {
			if (intentMaxScore.get(intentNamee) > 0.5 && intentMaxScore.get(intentNamee) > maxIntentScore) {
				maxIntentScore = intentMaxScore.get(intentNamee);
				intentName = intentNamee;
			}
		}
		logger.error("Classified intent: " + intentName);
		if (intentName == null)
			return null;
		Node findNodebyIntent = findNodebyIntent(intentName);
		return findNodebyIntent;
	}

	private double performCosineSimilarity(String userText, String standardText) {
		SortedMap<String, Double> standardMap = cosineDifferenceServices.getTermFrequencyMap(standardText);
		SortedMap<String, Integer> relativeTermFrequencyMap = cosineDifferenceServices
				.getRelativeTermFrequencyMap(userText, standardMap.keySet());
		SortedMap<String, Double> userMap = cosineDifferenceServices
				.convertAbsoluteFrequencyToRelativeFrequency(relativeTermFrequencyMap);
		int baseDictionarySize = standardMap.keySet().size();
		double[] vectorA = new double[baseDictionarySize];
		double[] vectorB = new double[baseDictionarySize];
		int i = 0;
		for (String s : standardMap.keySet()) {
			vectorA[i++] = standardMap.get(s);
		}
		i = 0;
		for (String s : userMap.keySet()) {
			Double userDictTF = userMap.get(s);
			if (userDictTF != null)
				vectorB[i++] = userDictTF;
		}
		double sentenceCosineSimilarity = cosineDifferenceServices.cosineSimilarity(vectorA, vectorB);
		if (Double.isNaN(sentenceCosineSimilarity))
			sentenceCosineSimilarity = 0;
		return sentenceCosineSimilarity;
	}

	private Edge getWeightedRandomEdgeSpeech(Node newNode) {
		ArrayList<Edge> edges = new ArrayList<>();
		edges.addAll(newNode.getEdges());
		int random = edges.size() > 1 ? new Random().nextInt(edges.size() - 1) : 0;
		return edges.get(random);
	}

	private void prepareIntentUtteranceDataFromDialogflowDump() {
		File intentFolder = new File("/var/www/html/salesdata/intents/insurance");
		File[] intentFiles = intentFolder.listFiles();
		for (File intentFile : intentFiles) {
			try {
				JsonObject intentJsonObject = jsonParser.parse(new FileReader(intentFile)).getAsJsonObject();
				String intentNamee = intentJsonObject.get("name").getAsString();
				HashSet<String> individualIntentUtterances = new HashSet<>();
				JsonArray userSays = intentJsonObject.get("userSays").getAsJsonArray();
				for (JsonElement userSayElement : userSays) {
					JsonObject userSayObject = userSayElement.getAsJsonObject();
					JsonArray userSayObjectDataJsonArray = userSayObject.get("data").getAsJsonArray();
					for (JsonElement userSayObjectDataJsonElement : userSayObjectDataJsonArray) {
						String text = userSayObjectDataJsonElement.getAsJsonObject().get("text").getAsString();
						individualIntentUtterances.add(text);
					}
				}
				intentUtterances.put(intentNamee, individualIntentUtterances);
			} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private Node findNodebyIntent(String intentName) {
		Node foundNode = null;
		for (Node node : experience.getNodes()) {
			if (node.getIntent().equalsIgnoreCase(intentName)) {
				foundNode = node;
				break;
			}
		}
		return foundNode;
	}

	private Experience getSampleExperience() throws JAXBException {
		Experience experience = null;
		File reader = new File("/var/www/html/salesdata/experience/1.xml");
		JAXBContext context = JAXBContext.newInstance(Experience.class);
		Unmarshaller createUnmarshaller = context.createUnmarshaller();
		experience = (Experience) createUnmarshaller.unmarshal(reader);
		return experience;
	}

}

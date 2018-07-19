package net.sourceforge.peers.demo;

import java.util.Scanner;

import talentify.ai.mitsuku.BotSingleton;
import talentify.ai.mitsuku.Chat;

public class MitsukuTest {
	public static void main(String[] args) {
		Chat chatSession = BotSingleton.getInstance().getChatS();
		Scanner scanner = new Scanner(System.in);
		System.out.println("\n");
		while (true) {
			System.out.print("Say Something:::\n");
			String query = scanner.nextLine();
			if (query.equalsIgnoreCase("q")) {
				scanner.close();
				break;
			} else {
				System.err.println(getMitsukuResponse(query, chatSession));
			}
		}
		System.exit(0);
	}

	private static String getMitsukuResponse(String query, Chat chatSession) {
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

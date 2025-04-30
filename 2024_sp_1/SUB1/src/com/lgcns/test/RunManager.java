package com.lgcns.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class RunManager {
	private static Map<String, String> dictionary = new HashMap<>();
	
	private static void loadDictionary() {
		try (BufferedReader br = new BufferedReader(new FileReader("DICTIONARY.TXT"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("#");
				if (parts.length == 2) {
					dictionary.put(parts[0], parts[1]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String processInput(String input) {
		String[] tokens = input.split(" ");
		StringBuilder result = new StringBuilder();
		
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i].toLowerCase();
			String embedding = dictionary.getOrDefault(token, "0,0,0");
			result.append(embedding);
			if (i < tokens.length - 1) {
				result.append(" ");
			}
		}
		
		return result.toString();
	}

	public static void main(String[] args) {
		loadDictionary();
		
		Scanner scanner = new Scanner(System.in);
		String input = scanner.nextLine();
		System.out.println(processInput(input));
		scanner.close();
	}
}







package com.lgcns.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class RunManager {
    private static Map<String, String> dictionary = new HashMap<>();
    private static Set<String> stopwords = new HashSet<>();

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

    private static void loadStopwords() {
        try (BufferedReader br = new BufferedReader(new FileReader("STOPWORD.TXT"))) {
            String line;
            while ((line = br.readLine()) != null) {
                stopwords.add(line.trim());
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
            if (!stopwords.contains(embedding)) {
                result.append(embedding);
                if (i < tokens.length - 1) {
                    result.append(" ");
                }
            }
        }

        return result.toString().trim();
    }

    public static void main(String[] args) {
        loadDictionary();
        loadStopwords();

        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        System.out.println(processInput(input));
        scanner.close();
    }
}
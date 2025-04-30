package com.lgcns.test;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class RunManager {
	private static final Gson gson = new Gson();
	private static Map<String, String> dictionary = new HashMap<>();
	private static Set<String> stopwords = new HashSet<>();
	private static JsonObject modelsConfig;

	public static void main(String[] args) throws Exception {
		// Load configuration files
		loadDictionary();
		loadStopwords();
		loadModelsConfig();

		// Create Jetty server
		Server server = new Server(8080);
		server.setHandler(new AbstractHandler() {
			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
				if (!request.getMethod().equals("POST")) {
					response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
					return;
				}

				try {
					// Read request body
					String requestBody = request.getReader().lines().collect(Collectors.joining());
					JsonObject requestJson = gson.fromJson(requestBody, JsonObject.class);

					// Process request
					String modelName = requestJson.get("modelname").getAsString();
					JsonArray queries = requestJson.get("queries").getAsJsonArray();
					List<String> results = new ArrayList<>();

					// Get model configuration
					JsonObject modelConfig = findModelConfig(modelName);
					if (modelConfig == null) {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						return;
					}

					// Process each query
					for (JsonElement query : queries) {
						String processedQuery = processQuery(query.getAsString());
						String classificationCode = classifyQuery(processedQuery, modelConfig.get("url").getAsString());
						String result = getClassificationValue(classificationCode, modelConfig);
						results.add(result);
					}

					// Prepare response
					JsonObject responseJson = new JsonObject();
					JsonArray resultsArray = new JsonArray();
					results.forEach(resultsArray::add);
					responseJson.add("results", resultsArray);
					String responseBody = gson.toJson(responseJson);

					// Send response
					response.setContentType("application/json");
					response.setStatus(HttpServletResponse.SC_OK);
					response.getWriter().write(responseBody);
					baseRequest.setHandled(true);

				} catch (Exception e) {
					e.printStackTrace();
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
		});

		server.start();
		System.out.println("Server started on port 8080");
		server.join();
	}

	private static void loadDictionary() throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader("./DICTIONARY.TXT"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("#");
				if (parts.length == 2) {
					dictionary.put(parts[0], parts[1]);
				}
			}
		}
	}

	private static void loadStopwords() throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader("./STOPWORD.TXT"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				stopwords.add(line);
			}
		}
	}

	private static void loadModelsConfig() throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader("./MODELS.JSON"))) {
			modelsConfig = gson.fromJson(reader, JsonObject.class);
		}
	}

	private static JsonObject findModelConfig(String modelName) {
		JsonArray models = modelsConfig.getAsJsonArray("models");
		for (JsonElement model : models) {
			JsonObject modelObj = model.getAsJsonObject();
			if (modelObj.get("modelname").getAsString().equals(modelName)) {
				return modelObj;
			}
		}
		return null;
	}

	private static String processQuery(String query) {
		// Tokenize and convert to lowercase
		String[] tokens = query.toLowerCase().split("\\s+");

		// Get embeddings and filter stopwords
		return Arrays.stream(tokens)
				.map(token -> dictionary.getOrDefault(token, ""))
				.filter(embedding -> !embedding.isEmpty() && !stopwords.contains(embedding))
				.collect(Collectors.joining(" "));
	}

	private static String classifyQuery(String processedQuery, String url) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(
						String.format("{\"query\":\"%s\"}", processedQuery)))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
		return responseJson.get("result").getAsString();
	}

	private static String getClassificationValue(String code, JsonObject modelConfig) {
		JsonArray classes = modelConfig.getAsJsonArray("classes");
		for (JsonElement cls : classes) {
			JsonObject classObj = cls.getAsJsonObject();
			if (classObj.get("code").getAsString().equals(code)) {
				return classObj.get("value").getAsString();
			}
		}
		return "unknown";
	}
}
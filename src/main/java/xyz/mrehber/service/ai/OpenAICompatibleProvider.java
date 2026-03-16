package xyz.mrehber.service.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * A generic provider for any AI that uses the OpenAI-compatible chat completions API.
 * This can be used for OpenAI, Groq, Perplexity, Together AI, Mistral (partial), etc.
 */
public class OpenAICompatibleProvider implements AIProvider {

    private final String name;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private static final int TIMEOUT = 30000;

    public OpenAICompatibleProvider(String name, String apiUrl, String apiKey, String model) {
        this.name = name;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String analyzeMessages(String prompt) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException(name + " API key is missing!");
        }

        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        body.add("messages", messages);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String error;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                error = br.lines().collect(Collectors.joining("\n"));
            }
            throw new IOException(name + " API Error (" + responseCode + "): " + error);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = br.lines().collect(Collectors.joining("\n"));
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }
    }
}

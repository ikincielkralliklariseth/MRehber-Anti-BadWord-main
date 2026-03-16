package xyz.mrehber.service.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.mrehber.manager.AISettings;
import xyz.mrehber.manager.ConfigManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class GroqProvider implements AIProvider {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final int TIMEOUT = 30000;

    private final ConfigManager configManager;

    public GroqProvider(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "Groq";
    }

    @Override
    public String analyzeMessages(String prompt) throws IOException {
        AISettings ai = configManager.getAiSettings();
        String apiKey = ai.getGroqKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Groq API key is missing!");
        }

        URL url = new URL(GROQ_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);

        JsonObject body = new JsonObject();
        body.addProperty("model", ai.getGroqModel());
        
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
            throw new IOException("Groq API Error (" + responseCode + "): " + error);
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

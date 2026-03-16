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

public class GeminiProvider implements AIProvider {

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final int TIMEOUT = 30000;

    private final ConfigManager configManager;

    public GeminiProvider(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "Gemini";
    }

    @Override
    public String analyzeMessages(String prompt) throws IOException {
        String apiKey = configManager.getAiSettings().getGeminiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Gemini API key is missing!");
        }

        URL url = new URL(GEMINI_API_URL + "?key=" + apiKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);

        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorMsg = "Bilinmeyen hata";
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                errorMsg = reader.lines().collect(Collectors.joining());
            } catch (Exception ignored) {}
            throw new IOException("Gemini API Error (" + responseCode + "): " + errorMsg);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseStr = reader.lines().collect(Collectors.joining());
            JsonObject jsonResponse = JsonParser.parseString(responseStr).getAsJsonObject();
            return jsonResponse.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        }
    }
}

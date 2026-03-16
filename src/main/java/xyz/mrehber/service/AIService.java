package xyz.mrehber.service;

import xyz.mrehber.manager.AISettings;
import xyz.mrehber.manager.ConfigManager;
import xyz.mrehber.service.ai.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AIService {

    private final ConfigManager configManager;
    private final String serverName;
    private final Map<String, AIProvider> providers = new HashMap<>();

    public AIService(ConfigManager configManager, String serverName) {
        this.configManager = configManager;
        this.serverName = serverName.toLowerCase();
        initializeProviders();
    }

    private void initializeProviders() {
        AISettings ai = configManager.getAiSettings();
        
        providers.put("gemini", new GeminiProvider(configManager));
        providers.put("groq", new GroqProvider(configManager));

        providers.put("openai", new OpenAICompatibleProvider("OpenAI",
                "https://api.openai.com/v1/chat/completions",
                ai.getOpenaiKey(), ai.getOpenaiModel()));

        providers.put("perplexity", new OpenAICompatibleProvider("Perplexity", 
                "https://api.perplexity.ai/chat/completions",
                ai.getPerplexityKey(), ai.getPerplexityModel()));

        providers.put("together", new OpenAICompatibleProvider("Together AI", 
                "https://api.together.xyz/v1/chat/completions",
                ai.getTogetherKey(), ai.getTogetherModel()));

        providers.put("mistral", new OpenAICompatibleProvider("Mistral AI", 
                "https://api.mistral.ai/v1/chat/completions",
                ai.getMistralKey(), ai.getMistralModel()));

        providers.put("deepseek", new OpenAICompatibleProvider("DeepSeek", 
                "https://api.deepseek.com/chat/completions",
                ai.getDeepSeekKey(), "deepseek-chat"));

        providers.put("openrouter", new OpenAICompatibleProvider("OpenRouter", 
                "https://openrouter.ai/api/v1/chat/completions",
                ai.getOpenRouterKey(), ai.getOpenRouterModel()));

        providers.put("cerebras", new OpenAICompatibleProvider("Cerebras", 
                "https://api.cerebras.ai/v1/chat/completions",
                ai.getCerebrasKey(), "llama3.1-70b"));
        
        providers.put("sambanova", new OpenAICompatibleProvider("SambaNova", 
                "https://api.sambanova.ai/v1/chat/completions",
                ai.getSambaNovaKey(), "Meta-Llama-3.1-405B-Instruct"));
        
        providers.put("deepinfra", new OpenAICompatibleProvider("DeepInfra", 
                "https://api.deepinfra.com/v1/openai/chat/completions",
                ai.getDeepInfraKey(), "meta-llama/Meta-Llama-3.1-405B-Instruct-Turbo"));

        providers.put("novita", new OpenAICompatibleProvider("Novita AI", 
                "https://api.novita.ai/v1/openai/chat/completions",
                ai.getNovitaKey(), "meta-llama/llama-3.1-70b-instruct"));
        
        providers.put("fireworks", new OpenAICompatibleProvider("Fireworks AI", 
                "https://api.fireworks.ai/inference/v1/chat/completions",
                ai.getFireworksKey(), "accounts/fireworks/models/llama-v3p1-70b-instruct"));
    }

    public String analyzeMessages(String messages) throws IOException {
        AISettings ai = configManager.getAiSettings();
        String activeProviderKey = ai.getProvider().toLowerCase();
        AIProvider provider = providers.getOrDefault(activeProviderKey, providers.get("gemini"));

        String prompt = ai.getPrompt()
                .replace("{server_name}", serverName)
                .replace("{messages}", messages);

        return analyzeWithRetry(provider, prompt);
    }

    private String analyzeWithRetry(AIProvider provider, String prompt) throws IOException {
        int maxRetries = 3;
        IOException lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                return provider.analyzeMessages(prompt);
            } catch (IOException e) {
                lastException = e;
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(1000 * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("İşlem iptal edildi", ie);
                    }
                }
            }
        }
        throw lastException;
    }
}

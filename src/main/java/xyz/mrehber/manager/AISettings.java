package xyz.mrehber.manager;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AISettings {
    private final boolean active;
    private final String provider;
    private final String prompt;
    
    private final String geminiKey;
    private final String groqKey;
    private final String groqModel;
    private final String openaiKey;
    private final String openaiModel;
    private final String perplexityKey;
    private final String perplexityModel;
    private final String togetherKey;
    private final String togetherModel;
    private final String mistralKey;
    private final String mistralModel;
    private final String openRouterKey;
    private final String openRouterModel;
    private final String deepSeekKey;
    private final String cerebrasKey;
    private final String sambaNovaKey;
    private final String deepInfraKey;
    private final String novitaKey;
    private final String fireworksKey;
}

# MRehber: AI Moderation System

<img width="640" height="840" alt="mrehber_banner_1773678876945" src="https://github.com/user-attachments/assets/7afaa186-0df7-4d6d-a740-f9595e4a5788" />

MRehber is a next-generation Minecraft moderation plugin that uses Artificial Intelligence to analyze chat context, identify toxic behavior, and maintain a respectful community automatically.

## Core Features

*   **Intelligent Analysis:** Integrates with leading AI APIs (Groq, Gemini, OpenAI, OpenRouter) for deep, contextual chat understanding.
*   **Graduated Punishments:** Automatically applies punishments based on offense severity:
    *   **Level 1:** Light profanity (Short Mutes)
    *   **Level 2:** Moderate toxicity (Mutes)
    *   **Level 3:** Severe offenses/Disrespect (Temporary Bans)
    *   *Suspicious messages are flagged for staff review.*
*   **Discord Integration:** Sends real-time moderation logs to your Discord webhooks.
*   **Spam Protection:** Built-in rate limiting and message history analysis.
*   **High Performance:** Fully asynchronous AI processing with robust Folia support for zero TPS impact.

## Installation

1.  Download the latest `MRehber.jar`.
2.  Place the plugin into your server's `plugins` directory.
3.  Start your server to generate the configuration files.
4.  Open `plugins/MRehber/config.yml` and enter your preferred AI provider's API key.
5.  Restart your server or reload the plugin.

## Basic Configuration

```yaml
sohbet-moderasyonu:
  ai:
    aktif: true
    provider: "groq" # Options: groq, gemini, openai, etc.

ai-providers:
  groq:
    model: "llama-3.3-70b-versatile" # Recommended default
```

## Support & Links

*   Developed by: **ikincielkralliklariseth**
*   GitHub Profile: [ikincielkralliklariseth](https://github.com/ikincielkralliklariseth)

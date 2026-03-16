package xyz.mrehber.manager;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import xyz.mrehber.MRehberPlugin;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ConfigManager {

    private final MRehberPlugin plugin;
    private final LanguageManager languageManager;
    private FileConfiguration config;

    private AISettings aiSettings;
    private final Map<String, PunishmentSettings> punishments = new HashMap<>();

    private int topluKontrolMesajSayisi;
    private int spamMaksimumMesaj;
    private long spamZamanAraligi;
    private int mesajGecmisiBoyutu;
    private int maksimumUyari;

    private boolean discordAktif;
    private String discordWebhookAdresi;

    private final String VARSAYILAN_ISTEM = """
            Aşağıdaki Minecraft sohbet mesajlarını analiz et. Sadece küfür, hakaret veya reklam içerenleri raporla.

            Derecelendirme Yazımı (MUTLAKA BU FORMATI KULLAN):
            - 1. DERECE → Normal küfür (sik, göt, am, orospu vb.)
            - 2. DERECE → Aileye küfür (ananı, bacını, babanı vb.)
            - 3. DERECE → Din, ırk, Atatürk, {server_name} gibi sunucuya hakaret
            - REKLAM    → Sunucu IP adresi, Web sitesi URL'si, Discord linki paylaşımı
            - UYARI     → Hafif hakaret (mal, salak, aptal, ezik vb.)

            ÖNEMLİ KURALLAR:
            - aq, sq, wtf, lol gibi kısaltmaları küfür sayMA!
            - qmk -> amk, qnqnıskm -> ananıskm gibi şifreli küfürleri çöz ve tespit et!
            - Reklam tespiti yaparken .com, .net, play., 192.168. gibi IP veya URL yapılarını ara.
            - Aynı oyuncudan birden fazla ihlal varsa sadece en ağır olanını yaz!
            - Her oyuncu için tek satır döndür!

            Format: OYUNCU_ADI|DERECE|ORİJİNAL_MESAJ
            Hiç ihlal yoksa sadece: TEMIZ

            Mesajlar:
            {messages}
            """;

    public ConfigManager(MRehberPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = new LanguageManager(plugin);
        yukle();
    }

    public void yukle() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        languageManager.load();

        loadGeneralSettings();
        loadAISettings();
        loadPunishments();
    }

    private void loadGeneralSettings() {
        topluKontrolMesajSayisi = Math.max(1, config.getInt("sohbet-moderasyonu.ayarlar.toplu-kontrol-mesaj-sayisi", 5));
        spamMaksimumMesaj = Math.max(1, config.getInt("sohbet-moderasyonu.ayarlar.spam-maksimum-mesaj", 8));
        spamZamanAraligi = Math.max(1000, config.getLong("sohbet-moderasyonu.ayarlar.spam-zaman-araligi", 10000));
        mesajGecmisiBoyutu = Math.max(10, config.getInt("sohbet-moderasyonu.ayarlar.mesaj-gecmisi-boyutu", 50));
        maksimumUyari = Math.max(1, config.getInt("uyari.maksimum-uyari", 3));

        discordAktif = config.getBoolean("sohbet-moderasyonu.discord.aktif", false);
        discordWebhookAdresi = config.getString("sohbet-moderasyonu.discord.webhook-adresi", "");

        if (discordAktif && (discordWebhookAdresi == null || discordWebhookAdresi.isBlank())) {
            plugin.getLogger().warning("Discord aktif ama webhook yok! Devre dışı bırakılıyor.");
            discordAktif = false;
        }
    }

    private void loadAISettings() {
        String configuredPrompt = config.getString("sohbet-moderasyonu.ai.istem");
        if (configuredPrompt == null || configuredPrompt.isBlank()) {
            configuredPrompt = VARSAYILAN_ISTEM;
        }

        aiSettings = AISettings.builder()
                .active(config.getBoolean("sohbet-moderasyonu.ai.aktif", true))
                .provider(config.getString("sohbet-moderasyonu.ai.provider", "gemini"))
                .prompt(configuredPrompt)
                .geminiKey(config.getString("ai-providers.gemini.api-anahtari", ""))
                .groqKey(config.getString("ai-providers.groq.api-anahtari", ""))
                .groqModel(config.getString("ai-providers.groq.model", "llama-3.3-70b-versatile"))
                .openaiKey(config.getString("ai-providers.openai.api-anahtari", ""))
                .openaiModel(config.getString("ai-providers.openai.model", "gpt-4o"))
                .perplexityKey(config.getString("ai-providers.perplexity.api-anahtari", ""))
                .perplexityModel(config.getString("ai-providers.perplexity.model", "llama-3.1-sonar-large-128k-online"))
                .togetherKey(config.getString("ai-providers.together.api-anahtari", ""))
                .togetherModel(config.getString("ai-providers.together.model", "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo"))
                .mistralKey(config.getString("ai-providers.mistral.api-anahtari", ""))
                .mistralModel(config.getString("ai-providers.mistral.model", "mistral-large-latest"))
                .openRouterKey(config.getString("ai-providers.openrouter.api-anahtari", ""))
                .openRouterModel(config.getString("ai-providers.openrouter.model", "meta-llama/llama-3.1-405b"))
                .deepSeekKey(config.getString("ai-providers.deepseek.api-anahtari", ""))
                .cerebrasKey(config.getString("ai-providers.cerebras.api-anahtari", ""))
                .sambaNovaKey(config.getString("ai-providers.sambanova.api-anahtari", ""))
                .deepInfraKey(config.getString("ai-providers.deepinfra.api-anahtari", ""))
                .novitaKey(config.getString("ai-providers.novita.api-anahtari", ""))
                .fireworksKey(config.getString("ai-providers.fireworks.api-anahtari", ""))
                .build();
    }

    private void loadPunishments() {
        punishments.put("1. DERECE", loadPunishment("cezalar.birinci-derece", "15m", "1. dereceden küfür"));
        punishments.put("2. DERECE", loadPunishment("cezalar.ikinci-derece", "30m", "2. dereceden küfür"));
        punishments.put("3. DERECE", loadPunishment("cezalar.ucuncu-derece", "7d", "3. dereceden küfür"));
        punishments.put("REKLAM", loadPunishment("cezalar.reklam", "30d", "Sunucu Reklamı"));
        punishments.put("BELIRSIZ", loadPunishment("cezalar.belirsizlik", "5m", "Hızlı Tespit Sistemi"));
        punishments.put("UYARI_LIMITI", loadPunishment("cezalar.uyari-sonrasi", "15m", "3 uyarı sonrası otomatik ceza"));
    }

    private PunishmentSettings loadPunishment(String path, String defDur, String defReason) {
        return PunishmentSettings.builder()
                .duration(config.getString(path + ".sure", defDur))
                .reason(config.getString(path + ".sebep", defReason))
                .command(config.getString(path + ".komut", "tempmute {oyuncu} {sure} {sebep}"))
                .build();
    }

    public String getMesaj(String path, String... placeholders) {
        return languageManager.getMessage(path, placeholders);
    }

    public String getRawMesaj(String path, String... placeholders) {
        return languageManager.getRawMessage(path, placeholders);
    }

    public String getGeminiIstem() {
        return aiSettings.getPrompt();
    }

    public String getGeminiApiAnahtari() {
        return aiSettings.getGeminiKey();
    }
}
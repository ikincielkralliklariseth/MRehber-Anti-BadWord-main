package xyz.mrehber;

import com.tcoded.folialib.FoliaLib;
import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mrehber.command.ReportCommand;
import xyz.mrehber.listener.ChatListener;
import xyz.mrehber.manager.ConfigManager;
import xyz.mrehber.service.AIService;
import xyz.mrehber.service.DiscordService;
import xyz.mrehber.service.ModerationService;
import xyz.mrehber.util.KufurDetector;

import java.util.logging.Level;

public final class MRehberPlugin extends JavaPlugin {

    @Getter
    private static MRehberPlugin instance;

    @Getter
    private ConfigManager configManager;
    @Getter
    private FoliaLib foliaLib;
    @Getter
    private AIService aiService;
    @Getter
    private DiscordService discordService;
    @Getter
    private ModerationService moderationService;
    @Getter
    private KufurDetector profanityDetector;
    @Getter
    private ChatListener chatListener;

    @Override
    public void onEnable() {
        instance = this;
        this.foliaLib = new FoliaLib(this);

        try {
            initialize();
            getLogger().info("MRehber eklentisi başarıyla yüklendi! (Folia: " + foliaLib.isFolia() + ")");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Eklenti yüklenirken bir hata oluştu:", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initialize() {
        this.configManager = new ConfigManager(this);
        
        this.aiService = new AIService(configManager, getServer().getName());
        this.discordService = new DiscordService(this, configManager);
        this.moderationService = new ModerationService(this, configManager, discordService);
        this.profanityDetector = new KufurDetector(this, configManager, getServer().getName());
        
        this.chatListener = new ChatListener(
                this,
                configManager,
                aiService,
                moderationService,
                profanityDetector,
                foliaLib);

        registerEvents(chatListener);
        registerCommands();
    }

    @Override
    public void onDisable() {
        if (chatListener != null) {
            chatListener.shutdown();
        }
        getLogger().info("MRehber eklentisi devre dışı bırakıldı.");
    }

    private void registerEvents(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void registerCommands() {
        registerCommand("bildir", new ReportCommand(chatListener, configManager));
        
        registerCommand("mrehberguncelle", (sender, command, label, args) -> {
            if (!sender.hasPermission("mrehber.admin")) {
                sender.sendMessage("§cBu komut için yetkiniz yok!");
                return true;
            }
            
            sender.sendMessage("§aKonfigürasyon ve filtreler güncelleniyor...");
            configManager.yukle();
            profanityDetector.yenidenYukle();
            sender.sendMessage("§aBaşarıyla güncellendi!");
            return true;
        });
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (executor instanceof TabCompleter) {
                command.setTabCompleter((TabCompleter) executor);
            }
        }
    }
}
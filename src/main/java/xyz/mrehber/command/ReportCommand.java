package xyz.mrehber.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.mrehber.listener.ChatListener;
import xyz.mrehber.manager.ConfigManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReportCommand implements CommandExecutor {

    private static final long BILDIRI_COOLDOWN = 180_000L;

    private final ChatListener chatListener;
    private final ConfigManager configManager;
    private final Map<String, Long> sonBildiriZamani = new ConcurrentHashMap<>();

    public ReportCommand(ChatListener chatListener, ConfigManager configManager) {
        this.chatListener = chatListener;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getMesaj("bildir.sadece-oyuncu"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(configManager.getMesaj("bildir.kullanim"));
            return true;
        }

        String hedefAdi = args[0];
        Player target = Bukkit.getPlayerExact(hedefAdi);

        if (target == null || !target.isOnline()) {
            player.sendMessage(configManager.getMesaj("bildir.oyuncu-yok"));
            return true;
        }

        String oyuncuAdi = player.getName();
        long now = System.currentTimeMillis();
        
        if (sonBildiriZamani.containsKey(oyuncuAdi) && !player.isOp()) {
            long diff = now - sonBildiriZamani.get(oyuncuAdi);
            if (diff < BILDIRI_COOLDOWN) {
                player.sendMessage(configManager.getMesaj("bildir.cooldown", "sure", formatDuration(BILDIRI_COOLDOWN - diff)));
                return true;
            }
        }

        player.sendMessage(configManager.getMesaj("bildir.isleniyor"));
        if (chatListener.oyuncuBildir(hedefAdi, player)) {
            sonBildiriZamani.put(oyuncuAdi, now);
        }

        return true;
    }

    private String formatDuration(long millis) {
        long s = millis / 1000;
        long m = s / 60;
        long sRem = s % 60;

        if (m == 0) return sRem + " saniye";
        if (sRem == 0) return m + " dakika";
        return m + " dakika " + sRem + " saniye";
    }
}
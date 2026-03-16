package xyz.mrehber.service;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import xyz.mrehber.MRehberPlugin;
import xyz.mrehber.manager.ConfigManager;
import xyz.mrehber.manager.PunishmentSettings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModerationService {

    private final MRehberPlugin plugin;
    private final ConfigManager configManager;
    private final DiscordService discordService;
    private final Map<UUID, Integer> oyuncuUyarilari = new ConcurrentHashMap<>();

    public ModerationService(MRehberPlugin plugin, ConfigManager configManager, DiscordService discordService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.discordService = discordService;
    }

    public void yanitIsle(String yanit, List<String> orijinalMesajlar, boolean efektOynandi) {
        if (yanit == null || yanit.trim().isEmpty() || yanit.trim().equals("TEMIZ")) return;

        Map<String, List<String[]>> oyuncuIhlalleri = new HashMap<>();
        for (String satir : yanit.split("\n")) {
            if (satir.trim().isEmpty() || satir.trim().equals("TEMIZ")) continue;
            String[] parca = satir.split("\\|", 3);
            if (parca.length >= 3) {
                oyuncuIhlalleri.computeIfAbsent(parca[0].trim(), k -> new ArrayList<>()).add(parca);
            }
        }

        for (Map.Entry<String, List<String[]>> entry : oyuncuIhlalleri.entrySet()) {
            String oyuncuAdi = entry.getKey();
            List<String[]> ihlaller = entry.getValue();

            String[] enCiddiIhlal = ihlaller.stream()
                    .max(Comparator.comparingInt(ihlal -> skor(ihlal[1])))
                    .orElse(null);

            if (enCiddiIhlal == null) continue;

            Player oyuncu = Bukkit.getPlayer(oyuncuAdi);
            if (oyuncu == null || !oyuncu.isOnline()) continue;

            if (oyuncu.hasPermission("moderasyon.bypass")) continue;

            if (!efektOynandi) {
                efektVeSes(oyuncu);
            }

            String derece = enCiddiIhlal[1].trim().toUpperCase();
            String mesaj = enCiddiIhlal[2].trim();

            switch (derece) {
                case "1", "1. DERECE" -> cezaUygula(oyuncu, configManager.getPunishments().get("1. DERECE"), mesaj, "1. derece küfür");
                case "2", "2. DERECE" -> cezaUygula(oyuncu, configManager.getPunishments().get("2. DERECE"), mesaj, "2. derece küfür");
                case "3", "3. DERECE" -> cezaUygula(oyuncu, configManager.getPunishments().get("3. DERECE"), mesaj, "3. derece küfür");
                case "REKLAM" -> cezaUygula(oyuncu, configManager.getPunishments().get("REKLAM"), mesaj, "Sunucu Reklamı");
                case "BELİRSİZLİK", "BELIRSIZ" -> cezaUygula(oyuncu, configManager.getPunishments().get("BELIRSIZ"), mesaj, "Yerel filtre belirsizliği");
                case "UYARI" -> uyariVer(oyuncu, mesaj);
                default -> cezaUygula(oyuncu, configManager.getPunishments().get("BELIRSIZ"), mesaj, "Bilinmeyen ihlal türü");
            }
        }
    }

    private int skor(String derece) {
        return switch (derece.toUpperCase()) {
            case "REKLAM" -> 5;
            case "3", "3. DERECE", "BELİRSİZLİK", "BELIRSIZ" -> 4;
            case "2", "2. DERECE" -> 3;
            case "1", "1. DERECE" -> 2;
            case "UYARI" -> 1;
            default -> 0;
        };
    }

    private void cezaUygula(Player oyuncu, PunishmentSettings settings, String mesaj, String discordAciklama) {
        if (settings == null) return;

        String finalKomut = settings.buildCommand(oyuncu.getName(), settings.getDuration(), settings.getReason());

        Bukkit.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalKomut);
        });

        oyuncu.sendMessage(configManager.getMesaj("ceza.susturuldu"));
        oyuncu.sendMessage(configManager.getMesaj("ceza.sebep", "sebep", settings.getReason()));
        oyuncu.sendMessage(configManager.getMesaj("ceza.sure", "sure", settings.getDuration()));

        oyuncu.sendTitle(
                configManager.getRawMesaj("ceza.title"),
                configManager.getRawMesaj("ceza.subtitle", "sure", settings.getDuration()),
                10, 80, 20
        );

        discordService.bildirimGonder(oyuncu.getName(), mesaj, discordAciklama, settings.getDuration());
        efektVeSes(oyuncu);
    }

    public void uyariVer(Player oyuncu, String mesaj) {
        UUID uuid = oyuncu.getUniqueId();
        int sayi = oyuncuUyarilari.getOrDefault(uuid, 0) + 1;
        oyuncuUyarilari.put(uuid, sayi);
        int maks = configManager.getMaksimumUyari();

        oyuncu.sendMessage(configManager.getMesaj("uyari.mesaj", "sayi", String.valueOf(sayi), "maksimum", String.valueOf(maks)));
        oyuncu.sendTitle(
                configManager.getRawMesaj("uyari.title"),
                configManager.getRawMesaj("uyari.subtitle", "sayi", String.valueOf(sayi), "maksimum", String.valueOf(maks)),
                10, 60, 15
        );

        if (sayi == 1) {
            oyuncu.sendMessage(configManager.getMesaj("ozur.ipucu"));
            oyuncu.playSound(oyuncu.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            oyuncu.spawnParticle(Particle.HAPPY_VILLAGER, oyuncu.getLocation().add(0, 2, 0), 15, 0.4, 0.4, 0.4, 0);
        }

        if (sayi >= maks) {
            cezaUygula(oyuncu, configManager.getPunishments().get("UYARI_LIMITI"), mesaj, maks + " uyarı sonrası otomatik ceza");
            oyuncuUyarilari.remove(uuid);
        } else {
            discordService.bildirimGonder(oyuncu.getName(), mesaj, sayi + ". uyarı", "Uyarı");
        }
    }

    private void efektVeSes(Player oyuncu) {
        oyuncu.spawnParticle(Particle.ANGRY_VILLAGER, oyuncu.getLocation().add(0, 2, 0), 40, 0.5, 0.5, 0.5, 0);
        oyuncu.spawnParticle(Particle.LARGE_SMOKE, oyuncu.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5, 0.06);
        oyuncu.playSound(oyuncu.getLocation(), Sound.ENTITY_WARDEN_ROAR, 1.2f, 0.8f);
    }

    public boolean ozurKabulEt(Player oyuncu) {
        UUID uuid = oyuncu.getUniqueId();
        int sayi = oyuncuUyarilari.getOrDefault(uuid, 0);
        if (sayi > 0) {
            oyuncuUyarilari.put(uuid, sayi - 1);
            oyuncu.sendMessage(configManager.getMesaj("ozur.kabul",
                    "sayi", String.valueOf(sayi - 1),
                    "maks", String.valueOf(configManager.getMaksimumUyari())));
            return true;
        }
        oyuncu.sendMessage(configManager.getMesaj("ozur.hic-uyari-yok"));
        return false;
    }

    public int getUyariSayisi(Player oyuncu) {
        return oyuncuUyarilari.getOrDefault(oyuncu.getUniqueId(), 0);
    }

    public void sifirlaUyari(Player oyuncu) {
        oyuncuUyarilari.remove(oyuncu.getUniqueId());
    }
}
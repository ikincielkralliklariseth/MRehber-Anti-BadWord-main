package xyz.mrehber.manager;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PunishmentSettings {
    private final String duration;
    private final String reason;
    private final String command;

    public String buildCommand(String player, String duration, String reason) {
        return command.replace("{oyuncu}", player)
                .replace("{sure}", duration)
                .replace("{sebep}", reason);
    }
}

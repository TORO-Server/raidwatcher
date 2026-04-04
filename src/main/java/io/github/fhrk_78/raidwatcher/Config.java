package io.github.fhrk_78.raidwatcher;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public record Config(
        HighRateLoginBlocker highRateLoginBlocker,
        SpamDetector spamDetector,
        ChatFilter chatFilter
) {
    static class IllegalConfigFormatException extends Exception {}

    static Config asConfig(FileConfiguration file) throws IllegalConfigFormatException {
        return new Config(
                new HighRateLoginBlocker(
                        file.getInt("HighRateLoginBlocker.allowed"),
                        file.getInt("HighRateLoginBlocker.per"),
                        file.getString("HighRateLoginBlocker.cooldown")
                ),
                new SpamDetector(
                        file.getInt("SpamDetector.allowed"),
                        file.getInt("SpamDetector.per"),
                        file.getString("SpamDetector.penalty.mute"),
                        file.getBoolean("SpamDetector.penalty.kick")
                ),
                new ChatFilter(
                        file.getStringList("ChatFilter.pattern"),
                        file.getString("ChatFilter.penalty.mute"),
                        file.getBoolean("ChatFilter.penalty.kick")
                )
        );
    }

    record HighRateLoginBlocker(
            int allowed,
            int per,
            String cooldown
    ) {}
    record SpamDetector(
            int allowed,
            int per,
            String penaltyMute,
            boolean penaltyKick
    ) {}
    record ChatFilter(
            List<String> pattern,
            String penaltyMute,
            boolean penaltyKick
    ) {}
}

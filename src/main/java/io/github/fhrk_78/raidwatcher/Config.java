package io.github.fhrk_78.raidwatcher;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.regex.Pattern;

public record Config(
        HighRateLoginBlocker highRateLoginBlocker,
        SpamDetector spamDetector,
        ChatFilter chatFilter,
        OneIPMultiAccountBlocker oneIPMultiAccountBlocker
) {
    static class IllegalConfigFormatException extends Exception {}

    static Config asConfig(FileConfiguration file) throws IllegalConfigFormatException {
        return new Config(
                new HighRateLoginBlocker(
                        file.getBoolean("HighRateLoginBlocker.enable"),
                        file.getInt("HighRateLoginBlocker.allowed"),
                        file.getInt("HighRateLoginBlocker.per"),
                        file.getString("HighRateLoginBlocker.cooldown")
                ),
                new SpamDetector(
                        file.getBoolean("SpamDetector.enable"),
                        file.getInt("SpamDetector.allowed"),
                        file.getInt("SpamDetector.per"),
                        file.getString("SpamDetector.penalty.mute"),
                        file.getBoolean("SpamDetector.penalty.kick")
                ),
                new ChatFilter(
                        file.getBoolean("ChatFilter.enable"),
                        file.getStringList("ChatFilter.pattern")
                                .stream()
                                .map(Pattern::compile)
                                .toList(),
                        file.getString("ChatFilter.penalty.mute"),
                        file.getBoolean("ChatFilter.penalty.kick")
                ),
                new OneIPMultiAccountBlocker(
                        file.getBoolean("OneIPMultiAccountBlocker.enable")
                )
        );
    }

    record HighRateLoginBlocker(
            boolean enable,
            int allowed,
            int per,
            String cooldown
    ) {}
    record SpamDetector(
            boolean enable,
            int allowed,
            int per,
            String penaltyMute,
            boolean penaltyKick
    ) {}
    record ChatFilter(
            boolean enable,
            List<Pattern> pattern,
            String penaltyMute,
            boolean penaltyKick
    ) {}
    record OneIPMultiAccountBlocker(
            boolean enable
    ) {}
}

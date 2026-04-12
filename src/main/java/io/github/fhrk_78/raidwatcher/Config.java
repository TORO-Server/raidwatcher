package io.github.fhrk_78.raidwatcher;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.regex.Pattern;

public record Config(
        HighRateLoginBlockerConfig highRateLoginBlockerConfig,
        SpamDetectorConfig spamDetectorConfig,
        ChatFilterConfig chatFilterConfig,
        OneIPMultiAccountBlockerConfig oneIPMultiAccountBlockerConfig
) {
    static class IllegalConfigFormatException extends Exception {}

    static Config asConfig(FileConfiguration file) throws IllegalConfigFormatException {
        return new Config(
                new HighRateLoginBlockerConfig(
                        file.getBoolean("HighRateLoginBlocker.enable"),
                        file.getInt("HighRateLoginBlocker.allowed"),
                        file.getInt("HighRateLoginBlocker.per"),
                        file.getString("HighRateLoginBlocker.cooldown")
                ),
                new SpamDetectorConfig(
                        file.getBoolean("SpamDetector.enable"),
                        file.getInt("SpamDetector.allowed"),
                        file.getInt("SpamDetector.per"),
                        file.getString("SpamDetector.penalty.mute"),
                        file.getBoolean("SpamDetector.penalty.kick")
                ),
                new ChatFilterConfig(
                        file.getBoolean("ChatFilter.enable"),
                        file.getStringList("ChatFilter.pattern")
                                .stream()
                                .map(Pattern::compile)
                                .toList(),
                        file.getString("ChatFilter.penalty.mute"),
                        file.getBoolean("ChatFilter.penalty.kick")
                ),
                new OneIPMultiAccountBlockerConfig(
                        file.getBoolean("OneIPMultiAccountBlocker.enable")
                )
        );
    }

    public record HighRateLoginBlockerConfig(
            boolean enable,
            int allowed,
            int per,
            String cooldown
    ) {}
    public record SpamDetectorConfig(
            boolean enable,
            int allowed,
            int per,
            String penaltyMute,
            boolean penaltyKick
    ) {}
    public record ChatFilterConfig(
            boolean enable,
            List<Pattern> pattern,
            String penaltyMute,
            boolean penaltyKick
    ) {}
    public record OneIPMultiAccountBlockerConfig(
            boolean enable
    ) {}
}

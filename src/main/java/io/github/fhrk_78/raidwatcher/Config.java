package io.github.fhrk_78.raidwatcher;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public record Config(
        HighRateLoginBlocker highRateLoginBlocker,
        SpamDetector spamDetector,
        ChatFilter chatFilter,
        List<String> bypassRole
) {
    static class IllegalConfigFormatException extends Exception {}

    static Config asConfig(FileConfiguration file) throws IllegalConfigFormatException {
        // バカみたいな検査のための式
        if (!(file.isInt("HighRateLoginBlocker.allowed")
                && file.isString("HighRateLoginBlocker.per")
                && Utils.isThisTimeText(file.getString("HighRateLoginBlocker.per"))
                && file.isString("HighRateLoginBlocker.cooldown")
                && Utils.isThisTimeText(file.getString("HighRateLoginBlocker.cooldown"))
                && file.isInt("SpamDetector.allowed")
                && file.isString("SpamDetector.per")
                && Utils.isThisTimeText(file.getString("SpamDetector.per"))
                && file.isString("SpamDetector.penalty.mute")
                && Utils.isThisTimeText(file.getString("SpamDetector.penalty.mute"))
                && file.isBoolean("SpamDetector.penalty.kick")
                && file.isList("ChatFilter.pattern")
                && file.isString("ChatFilter.penalty.mute")
                && Utils.isThisTimeText(file.getString("ChatFilter.penalty.mute"))
                && file.isBoolean("ChatFilter.penalty.kick")
                && file.isList("BypassRole"))) throw new IllegalConfigFormatException();

        return new Config(
                new HighRateLoginBlocker(
                        file.getInt("HighRateLoginBlocker.allowed"),
                        Utils.timeTextToSec(file.getString("HighRateLoginBlocker.per")),
                        Utils.timeTextToSec(file.getString("HighRateLoginBlocker.cooldown"))
                ),
                new SpamDetector(
                        file.getInt("SpamDetector.allowed"),
                        Utils.timeTextToSec(file.getString("SpamDetector.per")),
                        Utils.timeTextToSec(file.getString("SpamDetector.penalty.mute")),
                        file.getBoolean("SpamDetector.penalty.kick")
                ),
                new ChatFilter(
                        file.getStringList("ChatFilter.pattern"),
                        Utils.timeTextToSec(file.getString("ChatFilter.penalty.mute")),
                        file.getBoolean("ChatFilter.penalty.kick")
                ),
                file.getStringList("BypassRole")
        );
    }

    record HighRateLoginBlocker(
            int allowed,
            long per,
            long cooldown
    ) {}
    record SpamDetector(
            int allowed,
            long per,
            long penaltyMute,
            boolean penaltyKick
    ) {}
    record ChatFilter(
            List<String> pattern,
            long penaltyMute,
            boolean penaltyKick
    ) {}
}

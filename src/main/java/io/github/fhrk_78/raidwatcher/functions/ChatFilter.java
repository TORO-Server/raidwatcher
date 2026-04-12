package io.github.fhrk_78.raidwatcher.functions;

import io.github.fhrk_78.raidwatcher.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatFilter {
    Config config;

    public ChatFilter(Config config) {
        this.config = config;
    }

    public boolean isMessageMatched(Component message) {
        return config.chatFilterConfig()
                .pattern()
                .stream()
                .anyMatch(pattern -> pattern.matcher(
                        PlainTextComponentSerializer
                                .plainText()
                                .serialize(message)).find());
    }
}

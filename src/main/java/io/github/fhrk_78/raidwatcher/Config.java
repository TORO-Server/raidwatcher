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
                HighRateLoginBlocker.from(file),
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

    /**
     * @param allowed       per の間に、1つのアカウントがログインできる回数
     * @param per           数える長さ (tick)
     * @param cooldown      全員の入室を止める長さ
     * @param ipAllowed     per の間に、1つのIPからログインできる回数
     * @param globalAllowed per の間に、サーバー全体でログインできる回数。超えると入室を止める
     */
    record HighRateLoginBlocker(
            boolean enable,
            int allowed,
            int per,
            String cooldown,
            int ipAllowed,
            int globalAllowed
    ) {
        private static final int DEFAULT_ALLOWED = 3;
        private static final int DEFAULT_PER = 200;
        private static final int DEFAULT_IP_ALLOWED = 10;
        //TORO-Proxy が limbo から戻す速さの既定 (3秒に5人)。return-batch-size と check-interval-seconds のこと
        private static final int PROXY_RETURN_BATCH = 5;
        private static final int PROXY_RETURN_INTERVAL_TICKS = 60;

        static HighRateLoginBlocker from(FileConfiguration file) {
            final int allowed = Math.max(1, file.getInt("HighRateLoginBlocker.allowed", DEFAULT_ALLOWED));
            final int configuredPer = file.getInt("HighRateLoginBlocker.per", DEFAULT_PER);
            final int per = configuredPer > 0 ? configuredPer : DEFAULT_PER;
            //古い設定ファイルにはこの2つがない。jar の config.yml の値ではなく、per と allowed に合わせて決める
            final int ipAllowed = file.isSet("HighRateLoginBlocker.ipAllowed")
                    ? Math.max(1, file.getInt("HighRateLoginBlocker.ipAllowed"))
                    : Math.max(allowed, DEFAULT_IP_ALLOWED);
            final int globalAllowed = file.isSet("HighRateLoginBlocker.globalAllowed")
                    ? Math.max(1, file.getInt("HighRateLoginBlocker.globalAllowed"))
                    : Math.max(Math.max(allowed, ipAllowed) + 1, proxyReturns(per) * 2);
            return new HighRateLoginBlocker(
                    file.getBoolean("HighRateLoginBlocker.enable"),
                    allowed,
                    per,
                    file.getString("HighRateLoginBlocker.cooldown"),
                    ipAllowed,
                    globalAllowed
            );
        }

        /** per の間に TORO-Proxy が戻す、既定の最大の人数 */
        static int proxyReturns(int per) {
            return PROXY_RETURN_BATCH * (per / PROXY_RETURN_INTERVAL_TICKS + 1);
        }

        long windowMillis() {
            return per * 50L;
        }
    }
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

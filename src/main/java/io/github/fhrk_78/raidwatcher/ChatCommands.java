package io.github.fhrk_78.raidwatcher;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/** チャットのイベントを通らずに発言や私信を送るコマンド (LunaChat の /tell など) から、本文を取り出す */
final class ChatCommands {
    /** @param rateLimited 連投として数えるか。チャンネルのコマンドは発言以外にも使うので、禁止ワードだけ調べる */
    record Message(String text, boolean rateLimited) {}

    //宛先のあとに本文を書くコマンド (LunaChat、Essentials、バニラの別名を含む)
    private static final Set<String> TELL = Set.of(
            "tell", "msg", "message", "m", "t", "w", "whisper", "pm",
            "etell", "emsg", "ewhisper", "epm");
    //本文だけを書くコマンド
    private static final Set<String> REPLY = Set.of("reply", "r", "ereply", "er");
    //本文だけを書いて全体やチームへ発言するコマンド (バニラの /me、/teammsg と Essentials の別名を含む)
    private static final Set<String> BROADCAST = Set.of(
            "me", "eme", "action", "describe", "eaction", "edescribe",
            "teammsg", "tm");
    //Essentials のメール。本文を送るのは「/mail send 宛先 本文」の形だけ
    private static final Set<String> MAIL = Set.of("mail", "email");
    //「/ch チャンネル 本文」と「/ch join チャンネル 本文」でチャンネルに発言できるコマンド
    private static final Set<String> CHANNEL = Set.of("lunachat", "ch", "lc");

    private ChatCommands() {}

    /**
     * @param commandLine 「/」から始まるコマンドの全文
     * @return 本文。発言を送るコマンドでないか、本文がないときは null
     */
    static Message parse(String commandLine) {
        final String[] parts = commandLine.trim().split("\\s+");
        final String label = parts[0].startsWith("/") ? parts[0].substring(1) : parts[0];
        //「/plugin:tell」も同じコマンドとして扱う
        final String command = label.substring(label.lastIndexOf(':') + 1).toLowerCase(Locale.ROOT);

        if (TELL.contains(command)) return message(parts, 2, true);
        if (REPLY.contains(command)) return message(parts, 1, true);
        if (BROADCAST.contains(command)) return message(parts, 1, true);
        if (MAIL.contains(command)) {
            return parts.length > 1 && parts[1].equalsIgnoreCase("send") ? message(parts, 3, true) : null;
        }
        if (CHANNEL.contains(command)) {
            return message(parts, parts.length > 1 && parts[1].equalsIgnoreCase("join") ? 3 : 2, false);
        }
        return null;
    }

    private static Message message(String[] parts, int textStart, boolean rateLimited) {
        if (parts.length <= textStart) return null;
        return new Message(String.join(" ", Arrays.copyOfRange(parts, textStart, parts.length)), rateLimited);
    }
}

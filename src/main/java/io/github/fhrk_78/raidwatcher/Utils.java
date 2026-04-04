package io.github.fhrk_78.raidwatcher;

import java.util.regex.Pattern;

public class Utils {
    public static boolean isThisTimeText(String text) {
        return Pattern.compile("^(\\d+)(s|m|h|d|w|mo|y|)$").matcher(text).find();
    }

    public static long timeTextToSec(String text) {
        var match = Pattern.compile("^(\\d+)(s|m|h|d|w|mo|y|)$").matcher(text);
        if (match.find()) {
            int original = Integer.parseInt(match.group(1));
            switch (match.group(2)) {
                case "":
                case "s":
                    return original;
                case "m":
                    return original * 60L;
                case "h":
                    return original * 60L * 60L;
                case "d":
                    return original * 60L * 60L * 24L;
                case "w":
                    return original * 60L * 60L * 24L * 7L;
                case "mo":
                    return original * 60L * 60L * 24L * 30L;
                case "y":
                    return original * 60L * 60L * 24L * 365L;
            }
        }
        return 0L;
    }
}

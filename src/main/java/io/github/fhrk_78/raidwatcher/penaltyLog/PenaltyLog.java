package io.github.fhrk_78.raidwatcher.penaltyLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public abstract class PenaltyLog {
    public static ArrayList<PenaltyLog> logs = new ArrayList<>();

    public UUID uuid;
    public Instant timestamp;
}

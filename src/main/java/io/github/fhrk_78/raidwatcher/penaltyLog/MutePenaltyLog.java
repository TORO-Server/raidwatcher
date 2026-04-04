package io.github.fhrk_78.raidwatcher.penaltyLog;

import java.time.Instant;
import java.util.UUID;

public class MutePenaltyLog extends PenaltyLog {
    public Instant mute;

    public MutePenaltyLog(UUID uuid, Instant timestamp, Instant mute) {
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.mute = mute;
    }
}

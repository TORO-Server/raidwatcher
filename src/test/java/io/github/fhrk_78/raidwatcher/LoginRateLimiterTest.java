package io.github.fhrk_78.raidwatcher;

import io.github.fhrk_78.raidwatcher.LoginRateLimiter.Result;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {
    private static final long WINDOW = 10_000L;
    private static final long LOCKDOWN = 10_000L;

    /** 設定ファイルに新しいキーがないときの既定の値と同じ */
    private static LoginRateLimiter limiter() {
        return new LoginRateLimiter(3, 10, 40, WINDOW, LOCKDOWN, 0L);
    }

    @Test
    void oneAccountIsLimitedWithoutAffectingOthers() {
        LoginRateLimiter limiter = limiter();
        UUID account = UUID.randomUUID();
        for (int i = 0; i < 3; i++) assertEquals(Result.ALLOWED, limiter.check("ip" + i, account, 0L));
        assertEquals(Result.SOURCE_LIMITED, limiter.check("ip3", account, 0L));
        assertEquals(Result.ALLOWED, limiter.check("ip4", UUID.randomUUID(), 0L));
    }

    @Test
    void oneIpIsLimitedWithoutAffectingOthers() {
        LoginRateLimiter limiter = limiter();
        for (int i = 0; i < 10; i++) assertEquals(Result.ALLOWED, limiter.check("shared", UUID.randomUUID(), 0L));
        assertEquals(Result.SOURCE_LIMITED, limiter.check("shared", UUID.randomUUID(), 0L));
        assertEquals(Result.ALLOWED, limiter.check("other", UUID.randomUUID(), 0L));
    }

    @Test
    void limitedSourceCannotStartLockdownHoweverOftenItTries() {
        LoginRateLimiter limiter = limiter();
        UUID account = UUID.randomUUID();
        for (int i = 0; i < 10_000; i++) assertNotEquals(Result.LOCKDOWN_STARTED, limiter.check("one", account, i % WINDOW));
        for (int i = 0; i < 10_000; i++) assertNotEquals(Result.LOCKDOWN_STARTED, limiter.check("one", UUID.randomUUID(), i % WINDOW));
    }

    @Test
    void manyDifferentSourcesStartLockdownOverTheGlobalLimit() {
        LoginRateLimiter limiter = limiter();
        for (int i = 0; i < 40; i++) assertEquals(Result.ALLOWED, limiter.check("ip" + i, UUID.randomUUID(), 0L));
        assertEquals(Result.LOCKDOWN_STARTED, limiter.check("ip40", UUID.randomUUID(), 0L));
        assertEquals(Result.LOCKED_DOWN, limiter.check("ip41", UUID.randomUUID(), 1L));
    }

    @Test
    void attemptsDuringLockdownDoNotExtendIt() {
        LoginRateLimiter limiter = limiter();
        for (int i = 0; i <= 40; i++) limiter.check("ip" + i, UUID.randomUUID(), 0L);
        for (long now = 1L; now < LOCKDOWN; now += 100L) {
            assertEquals(Result.LOCKED_DOWN, limiter.check("late" + now, UUID.randomUUID(), now));
        }
        assertEquals(Result.ALLOWED, limiter.check("after", UUID.randomUUID(), LOCKDOWN));
    }

    @Test
    void countsStartOverInTheNextWindow() {
        LoginRateLimiter limiter = limiter();
        UUID account = UUID.randomUUID();
        for (int i = 0; i < 3; i++) limiter.check("ip", account, 0L);
        assertEquals(Result.SOURCE_LIMITED, limiter.check("ip", account, WINDOW - 1));
        assertEquals(Result.ALLOWED, limiter.check("ip", account, WINDOW));
    }

    @Test
    void proxyReturnRateNeverStartsLockdown() {
        // TORO-Proxy の既定: 3秒ごとに5人を戻す。時間の区切りに関係なく、全員が違う IP のときも同じ IP のときも確かめる
        for (long offset = 0L; offset < WINDOW; offset += 500L) {
            LoginRateLimiter distinct = limiter();
            LoginRateLimiter shared = limiter();
            for (int batch = 0; batch < 200; batch++) {
                long now = offset + batch * 3_000L;
                for (int player = 0; player < 5; player++) {
                    assertEquals(Result.ALLOWED, distinct.check("ip" + batch + "-" + player, UUID.randomUUID(), now));
                    assertNotEquals(Result.LOCKDOWN_STARTED, shared.check("shared", UUID.randomUUID(), now));
                }
            }
        }
    }

    @Test
    void lockdownEndWorksWhenTheClockIsNegative() {
        // System.nanoTime は負の値から始まることがある
        LoginRateLimiter limiter = new LoginRateLimiter(3, 10, 1, WINDOW, LOCKDOWN, -100_000L);
        assertEquals(Result.ALLOWED, limiter.check("a", UUID.randomUUID(), -100_000L));
        assertEquals(Result.LOCKDOWN_STARTED, limiter.check("b", UUID.randomUUID(), -100_000L));
        assertEquals(Result.LOCKED_DOWN, limiter.check("c", UUID.randomUUID(), -100_000L + LOCKDOWN - 1));
        assertEquals(Result.ALLOWED, limiter.check("d", UUID.randomUUID(), -100_000L + LOCKDOWN));
    }
}

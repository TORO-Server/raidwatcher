package io.github.fhrk_78.raidwatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 短い時間のログインを、アカウントごと、IPごと、サーバー全体で数える。
 * アカウントとIPの上限を超えたログインはその人だけ断り、全体の数には入れないので、少ない接続で全員を止めることはできない。
 * 時刻は引数で受け取るので、サーバーなしで確かめられる
 */
final class LoginRateLimiter {
    enum Result {
        ALLOWED,
        /** このアカウントかIPが上限を超えた。全体の入室制限ではない */
        SOURCE_LIMITED,
        /** 全体の上限を超えたので、入室制限を始めた */
        LOCKDOWN_STARTED,
        /** 入室制限のあいだ */
        LOCKED_DOWN
    }

    private final int accountAllowed;
    private final int ipAllowed;
    private final int globalAllowed;
    private final long windowMillis;
    private final long lockdownMillis;

    private final Map<UUID, Integer> accountCounts = new HashMap<>();
    private final Map<String, Integer> ipCounts = new HashMap<>();
    private int globalCount = 0;
    private long windowStart;
    private long lockdownEnd;

    /** @param now 時刻 (ミリ秒)。{@link #check} と同じ時計を使う */
    LoginRateLimiter(int accountAllowed, int ipAllowed, int globalAllowed, long windowMillis, long lockdownMillis, long now) {
        this.accountAllowed = accountAllowed;
        this.ipAllowed = ipAllowed;
        this.globalAllowed = globalAllowed;
        this.windowMillis = windowMillis;
        this.lockdownMillis = lockdownMillis;
        this.windowStart = now;
        this.lockdownEnd = now;
    }

    synchronized Result check(String ip, UUID account, long now) {
        //入室制限のあいだの試行は数えないので、制限が延びることはない
        if (now - lockdownEnd < 0) return Result.LOCKED_DOWN;

        if (now - windowStart >= windowMillis) reset(now);

        if (accountCounts.getOrDefault(account, 0) >= accountAllowed
                || ipCounts.getOrDefault(ip, 0) >= ipAllowed) {
            return Result.SOURCE_LIMITED;
        }
        accountCounts.merge(account, 1, Integer::sum);
        ipCounts.merge(ip, 1, Integer::sum);

        if (++globalCount > globalAllowed) {
            lockdownEnd = now + lockdownMillis;
            //制限が終わったら数え直す
            reset(now);
            return Result.LOCKDOWN_STARTED;
        }
        return Result.ALLOWED;
    }

    private void reset(long now) {
        accountCounts.clear();
        ipCounts.clear();
        globalCount = 0;
        windowStart = now;
    }
}

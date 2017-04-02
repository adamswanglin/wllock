package org.wl.wllock.lock.distribution;

/**
 * Created by wanglin on 2017/3/28.
 */
public interface DistributionLock {

    /**
     * 获取分布式锁
     *
     * @param key             锁id
     * @param tryMilliSeconds 限定时长
     * @return 锁结果
     */
    boolean tryLock(String key, int tryMilliSeconds);

    /**
     * 解锁
     *
     * @param key key值
     */
    void unlock(String key);
}

package org.wl.wllock.lock.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wl.wllock.aspect.LockAspect;
import org.wl.wllock.config.LockSettings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by wanglin on 2017/3/28.
 */
@Component
public class LocalLock {

    @Autowired
    private LockSettings lockSettings;
    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(LockAspect.class);

    /**
     * 获取本地锁
     *
     * @param key             锁id
     * @param tryMilliSeconds 最大等待时长
     * @return 锁结果
     */
    public LocalLockResult tryLock(String key, int tryMilliSeconds) {
        //获取ReentrantLock
        LocalLockResult localLockResult = new LocalLockResult();
        ReentrantLock reentrantLock = LOCK_MAP.get(key);
        if (reentrantLock == null) {
            reentrantLock = new ReentrantLock(true);
            ReentrantLock existed = LOCK_MAP.putIfAbsent(key, reentrantLock);
            if (existed != null) {
                reentrantLock = existed;
            }
        }
        localLockResult.setLock(reentrantLock);
        if (reentrantLock.getQueueLength() <= lockSettings.getSingleWaitThreshold()) {
            //获取本地锁
            try {
                if (tryMilliSeconds > 0) {
                    if (reentrantLock.tryLock(tryMilliSeconds, TimeUnit.MILLISECONDS)) {
                        localLockResult.setLockSuccess(true);
                    }
                } else {
                    reentrantLock.lock();
                    localLockResult.setLockSuccess(true);
                }
            } catch (InterruptedException e) {
                LOGGER.error("get local lock {} in {} miliseconds interupted", key, tryMilliSeconds);
            }
        }
        return localLockResult;
    }

    /**
     * 释放本地锁
     *
     * @param key             锁id
     * @param localLockResult 锁结果
     */
    public void unlock(String key, LocalLockResult localLockResult) {
        if (localLockResult != null && localLockResult.getLock() != null) {
            if (localLockResult.isLockSuccess()) {
                localLockResult.getLock().unlock();
            }

            //当前key没有线程等待锁，删除
            if (!localLockResult.getLock().hasQueuedThreads()) {
                LOCK_MAP.remove(key, localLockResult.getLock());
            }
        }
    }
}

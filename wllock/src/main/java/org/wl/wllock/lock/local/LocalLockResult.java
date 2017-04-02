package org.wl.wllock.lock.local;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地锁结果
 * Created by wanglin on 2017/3/28.
 */
public class LocalLockResult {
    /**
     * 是否锁成功
     */
    private boolean lockSuccess;

    /**
     * 锁
     */
    private ReentrantLock lock;

    /**
     * 信号量
     */
    private Semaphore semaphore;

    public boolean isLockSuccess() {
        return lockSuccess;
    }

    public void setLockSuccess(boolean lockSuccess) {
        this.lockSuccess = lockSuccess;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void setLock(ReentrantLock lock) {
        this.lock = lock;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }
}

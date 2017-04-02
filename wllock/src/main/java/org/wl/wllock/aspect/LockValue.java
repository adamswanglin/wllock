package org.wl.wllock.aspect;


import org.wl.wllock.aspect.annotation.LockValueAnnotation;
import org.wl.wllock.lock.local.LocalLockResult;

/**
 * Created by wanglin on 2017/3/29.
 */
@LockValueAnnotation
public class LockValue {

    public LockValue(String key) {
        this.key = key;
    }

    /**
     * 锁标识
     */
    private String key;

    /**
     * 是否获取锁成功
     */
    private boolean lockSuccess;

    /**
     * 获取锁时间(ms)，超时返回获取锁失败
     */
    private int tryLockMilliSeconds;

    /**
     * 锁信息
     */
    private LockDetail lockDetail;

    public static class LockDetail {
        /**
         * 本地锁是否获取成功
         */
        private boolean localLock;

        /**
         * 分布锁是否获取成功
         */
        private boolean distributionLock;

        /**
         * 获取本地锁时间
         */
        private long localLockTime;

        /**
         * 获取分布锁时间
         */
        private long distributionLockTime;

        /**
         * 本地锁结果
         */
        private LocalLockResult localLockResult;

        public boolean isLocalLock() {
            return localLock;
        }

        public void setLocalLock(boolean localLock) {
            this.localLock = localLock;
        }

        public boolean isDistributionLock() {
            return distributionLock;
        }

        public void setDistributionLock(boolean distributionLock) {
            this.distributionLock = distributionLock;
        }

        public long getLocalLockTime() {
            return localLockTime;
        }

        public void setLocalLockTime(long localLockTime) {
            this.localLockTime = localLockTime;
        }

        public long getDistributionLockTime() {
            return distributionLockTime;
        }

        public void setDistributionLockTime(long distributionLockTime) {
            this.distributionLockTime = distributionLockTime;
        }

        public LocalLockResult getLocalLockResult() {
            return localLockResult;
        }

        public void setLocalLockResult(LocalLockResult localLockResult) {
            this.localLockResult = localLockResult;
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isLockSuccess() {
        return lockSuccess;
    }

    public void setLockSuccess(boolean lockSuccess) {
        this.lockSuccess = lockSuccess;
    }

    public int getTryLockMilliSeconds() {
        return tryLockMilliSeconds;
    }

    public void setTryLockMilliSeconds(int tryLockMilliSeconds) {
        this.tryLockMilliSeconds = tryLockMilliSeconds;
    }

    public LockDetail getLockDetail() {
        return lockDetail;
    }

    public void setLockDetail(LockDetail lockDetail) {
        this.lockDetail = lockDetail;
    }
}

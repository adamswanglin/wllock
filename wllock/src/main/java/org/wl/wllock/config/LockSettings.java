package org.wl.wllock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by wanglin6 on 2017/3/28.
 */
@ConfigurationProperties(prefix = "wllock")
public class LockSettings {
    /**
     * 系统名称
     */
    private String systemName;

    /**
     * 最大锁等待时间
     */
    private int lockTimeMaxMillis = 5000;

    /**
     * 单机等待阈值
     * 超过阈值服务降级，默认1000
     */
    private int singleWaitThreshold = 1000;


    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public int getLockTimeMaxMillis() {
        return lockTimeMaxMillis;
    }

    public void setLockTimeMaxMillis(int lockTimeMaxMillis) {
        this.lockTimeMaxMillis = lockTimeMaxMillis;
    }

    public int getSingleWaitThreshold() {
        return singleWaitThreshold;
    }

    public void setSingleWaitThreshold(int singleWaitThreshold) {
        this.singleWaitThreshold = singleWaitThreshold;
    }
}

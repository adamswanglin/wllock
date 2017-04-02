package org.wl.wllock.lock.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.wl.wllock.config.LockSettings;

import java.util.concurrent.TimeUnit;

/**
 * Created by wanglin on 2017/3/28.
 */
@Component
public class RedisLock implements DistributionLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLock.class);

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private LockSettings lockSettings;

    public boolean tryLock(String key, int tryMilliSeconds) {
        try {
            long start = System.currentTimeMillis();
            while (true) {
                if (redisTemplate.opsForValue().setIfAbsent(key, "")) {
                    redisTemplate.expire(key, lockSettings.getLockTimeMaxMillis(), TimeUnit.MILLISECONDS);
                    return true;
                }
                long end = System.currentTimeMillis();
                if (tryMilliSeconds > 0 && tryMilliSeconds < (end - start)) {
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("get distributionlock {} error", key, e);
        }
        return false;
    }

    public void unlock(String key) {
        redisTemplate.delete(key);
    }

}

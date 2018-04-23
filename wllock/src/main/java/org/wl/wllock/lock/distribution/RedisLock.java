package org.wl.wllock.lock.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.wl.wllock.config.LockSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wanglin on 2017/3/28.
 */
@Component
public class RedisLock implements DistributionLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLock.class);

    /**
     * setNX + expire的lua脚本.
     * <p>
     * setNxWithExpire(key, args[1], args[2]).
     * key
     * args[1] 值
     * args[2] 失效时间，单位：毫秒
     */
    private static final String SET_NX_WITH_EXPIRE =
            "local rst = redis.call('SETNX',KEYS[1],ARGV[1]);"
                    + "if (rst==1) then redis.call('PEXPIRE', KEYS[1], ARGV[2]); end;"
                    + " return rst;";

    /**
     * setNX + expire脚本.
     */
    private static RedisScript<Long> SET_NX_WITH_EXPIRE_SCRIPT = new DefaultRedisScript<>(SET_NX_WITH_EXPIRE, Long.class);
    private static RedisSerializer<Long> LONG_SERIALIZER = new GenericToStringSerializer<>(Long.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LockSettings lockSettings;

    public boolean tryLock(String key, int tryMilliSeconds) {
        try {
            long start = System.currentTimeMillis();
            while (true) {
                List<String> keys = new ArrayList<>(1);
                keys.add(key);
                String value = "";

                Long result = redisTemplate.execute(SET_NX_WITH_EXPIRE_SCRIPT, redisTemplate.getStringSerializer(), LONG_SERIALIZER, keys, value, String.valueOf(lockSettings.getLockTimeMaxMillis()));
                if (result == 1) {
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

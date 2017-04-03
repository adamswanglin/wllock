package org.wl.wllock.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wl.wllock.aspect.annotation.LockGuard;
import org.wl.wllock.config.LockSettings;
import org.wl.wllock.lock.distribution.DistributionLock;
import org.wl.wllock.lock.local.LocalLock;
import org.wl.wllock.lock.local.LocalLockResult;

/**
 * Created by wanglin on 2017/3/28.
 */
@Aspect
@Component
public class LockAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(LockAspect.class);

    @Autowired
    private LockSettings lockSettings;

    @Autowired
    private DistributionLock distributionLock;

    @Autowired
    private LocalLock localLock;


    /***
     * 加锁aspect
     * @param joinPoint 连接点
     * @param lockAnnotation 方法需要带注解 例如LockGuard(name="product")
     * @param lockValue 方法第一个参数是 LockValue
     * @throws Throwable
     */
    @Around("@annotation(lockAnnotation) && args(lockValue, ..)")
    public void aroundAction(JoinPoint joinPoint, LockGuard lockAnnotation, LockValue lockValue) throws Throwable {
        String key = lockSettings.getSystemName() + ":" + lockAnnotation.name() + ":" + lockValue.getKey();
        LockValue.LockDetail lockDetail = null;
        try {
            lockDetail = tryLock(key, lockValue.getTryLockMilliSeconds());
            lockValue.setLockSuccess(lockDetail.isDistributionLock());
            lockValue.setLockDetail(lockDetail);

            ((ProceedingJoinPoint) joinPoint).proceed();

        } catch (InterruptedException e) {
            LOGGER.error("get {} interupted", key);
        } finally {
            try {
                if (lockDetail != null && lockDetail.isDistributionLock()) {
                    distributionLock.unlock(key);
                }
            } catch (Exception e) {
                LOGGER.error("unlock distribution {} error", key, e);
            }

            try {
                if (lockDetail != null && lockDetail.isLocalLock()) {
                    localLock.unlock(key, lockDetail.getLocalLockResult());
                }
            } catch (Exception e) {
                LOGGER.error("unlock local {} error", key, e);
            }
        }
    }

    /**
     * 获取本地锁和分布式锁
     *
     * @param key          锁id
     * @param milliSeconds 限时 ms
     * @return
     * @throws InterruptedException
     */
    private LockValue.LockDetail tryLock(String key, long milliSeconds) throws InterruptedException {
        long start = System.currentTimeMillis();
        LockValue.LockDetail lockDetail = new LockValue.LockDetail();
        //先获取本地锁
        LocalLockResult localLockResult = localLock.tryLock(key, (int) milliSeconds);
        long local = System.currentTimeMillis();
        boolean distributionLockSuccess = false;
        if (localLockResult.isLockSuccess()) {
            long leftMiliSeconds = milliSeconds - (local - start);
            if (milliSeconds == 0 || leftMiliSeconds > 0) {
                //再获取分布式锁
                distributionLockSuccess = distributionLock.tryLock(key, milliSeconds == 0 ? 0 : (int) leftMiliSeconds / 2);
            }
        }
        long distribute = System.currentTimeMillis();
        //记录锁详细信息
        lockDetail.setLocalLockResult(localLockResult);
        lockDetail.setLocalLock(localLockResult.isLockSuccess());
        lockDetail.setDistributionLock(distributionLockSuccess);
        lockDetail.setLocalLockTime(local - start);
        lockDetail.setDistributionLockTime(distribute - local);
        LOGGER.debug("tryLock {} {} , use time : {} ms ", key, lockDetail.isDistributionLock(), System.currentTimeMillis() - start);
        return lockDetail;
    }
}

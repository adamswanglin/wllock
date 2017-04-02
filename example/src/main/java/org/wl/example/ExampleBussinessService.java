package org.wl.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wl.wllock.aspect.LockValue;
import org.wl.wllock.aspect.annotation.LockGuard;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wanglin on 2017/3/29.
 */
@Service
public class ExampleBussinessService {
    private static Logger LOGGER = LoggerFactory.getLogger(ExampleBussinessService.class);
    public static int i;
    public static ConcurrentHashMap<String, LockStatistics> STATISTIC_MAP = new ConcurrentHashMap<>();

    @LockGuard(name = "tempLock")
    public LockValue.LockDetail getLock(LockValue lockValue, String id) throws Exception {
        LockValue.LockDetail lockDetail = lockValue.getLockDetail();

        if (lockValue.isLockSuccess()) {
            //获取锁成功，业务处理
            Thread.sleep(100);
        } else {
            //获取锁失败
        }

        //统计锁信息
        LockStatistics newStatistics = new LockStatistics();
        LockStatistics statistics = STATISTIC_MAP.putIfAbsent(lockValue.getKey(), newStatistics);
        if (statistics == null) {
            statistics = newStatistics;
        }
        statistics.getTotal().incrementAndGet();
        if (lockDetail.isLocalLock()) {
            statistics.getLocalSuccess().incrementAndGet();
            statistics.getTotalLocalTime().addAndGet(lockDetail.getLocalLockTime());
        }
        if (lockDetail.isDistributionLock()) {
            statistics.getDistributeSuccess().incrementAndGet();
            statistics.getTotalDistributeTime().addAndGet(lockDetail.getDistributionLockTime());
        }
        //LOGGER.info("lockDetail isDistributionLock {}", lockDetail.isDistributionLock());
        return lockValue.getLockDetail();
    }


    public static class LockStatistics {
        private AtomicLong totalLocalTime = new AtomicLong();
        private AtomicLong totalDistributeTime = new AtomicLong();

        private AtomicInteger localSuccess = new AtomicInteger();
        private AtomicInteger distributeSuccess = new AtomicInteger();
        private AtomicInteger total = new AtomicInteger();

        public AtomicLong getTotalLocalTime() {
            return totalLocalTime;
        }

        public void setTotalLocalTime(AtomicLong totalLocalTime) {
            this.totalLocalTime = totalLocalTime;
        }

        public AtomicLong getTotalDistributeTime() {
            return totalDistributeTime;
        }

        public void setTotalDistributeTime(AtomicLong totalDistributeTime) {
            this.totalDistributeTime = totalDistributeTime;
        }

        public AtomicInteger getLocalSuccess() {
            return localSuccess;
        }

        public void setLocalSuccess(AtomicInteger localSuccess) {
            this.localSuccess = localSuccess;
        }

        public AtomicInteger getDistributeSuccess() {
            return distributeSuccess;
        }

        public void setDistributeSuccess(AtomicInteger distributeSuccess) {
            this.distributeSuccess = distributeSuccess;
        }

        public AtomicInteger getTotal() {
            return total;
        }

        public void setTotal(AtomicInteger total) {
            this.total = total;
        }
    }
}

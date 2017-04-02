package org.wl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.wl.example.ExampleBussinessService;
import org.wl.wllock.aspect.LockValue;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ExampleTests {
    static Logger logger = LoggerFactory.getLogger(ExampleTests.class);
    @Autowired
    private ExampleBussinessService example;

    @Test
    public void test() {

        List<LockValue> lockValues = new ArrayList<>();

        LockValue lockValue0 = new LockValue("0000");
        lockValues.add(lockValue0);

        LockValue lockValue1 = new LockValue("1111");
        lockValue1.setTryLockMilliSeconds(5000);
        lockValues.add(lockValue1);

        CountDownLatch fireLatch = new CountDownLatch(1);
        int iterator = 100;
        CountDownLatch finishLatch = new CountDownLatch(iterator * lockValues.size());
        for (LockValue value : lockValues) {
            ThreadGroup threadGroup = new ThreadGroup("lock-" + value.getKey());
            for (int i = 0; i < iterator; i++) {
                Thread thread = new Thread(threadGroup, () -> {
                    try {
                        fireLatch.await();
                    } catch (InterruptedException e) {
                    }
                    try {
                        LockValue newLockValue = new LockValue(value.getKey());
                        newLockValue.setTryLockMilliSeconds(value.getTryLockMilliSeconds());
                        LockValue.LockDetail lockDetail = example.getLock(newLockValue, null);
                        if (lockDetail != null)
                            logger.info("local {} {} distribution {} {}", lockDetail.isLocalLock(), lockDetail.getLocalLockTime(), lockDetail.isDistributionLock(), lockDetail.getDistributionLockTime());

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        finishLatch.countDown();
                    }
                });

                thread.start();
            }
        }
        fireLatch.countDown();
        try {
            finishLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (ExampleBussinessService.STATISTIC_MAP != null) {
            logger.info("key\t lcSuccess\t dsSuccess\t localTime\t distributeTime");
            for (Map.Entry<String, ExampleBussinessService.LockStatistics> stringLockStatisticsEntry : ExampleBussinessService.STATISTIC_MAP.entrySet()) {
                ExampleBussinessService.LockStatistics statistics = stringLockStatisticsEntry.getValue();
                logger.info("{}\t {}\t {}\t {}\t {}\t {}"
                        , stringLockStatisticsEntry.getKey()
                        , ((double) statistics.getLocalSuccess().get()) / iterator
                        , ((double) statistics.getDistributeSuccess().get()) / iterator
                        , new DecimalFormat("#.00").format(((double) statistics.getTotalLocalTime().get()) / statistics.getLocalSuccess().get())
                        , new DecimalFormat("#.00").format(((double) statistics.getTotalDistributeTime().get()) / statistics.getDistributeSuccess().get())
                        , statistics.getTotal().get()
                );
            }
        }
    }
}

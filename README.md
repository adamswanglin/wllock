#1. 解决什么问题
考虑分布式商品秒杀系统，如何保证商品数量不超卖？加锁呗，一般会这样想：



## 1.1.  MySQL乐观锁和悲观锁

#### 乐观锁和悲观锁的实现

假设，MySQL数据库中商品库存表product_stock结构如下： 

| 字段         | 备注                   |
| ---------- | -------------------- |
| product_id | 商品id, 唯一索引unique_key |
| number     | 商量剩余数量               |

通过给商品库存加乐观锁，判断秒杀是否成功的伪代码:

```java
/**
* 该方法返回是否秒杀成功
* @param productId 产品ID
* @return 是否成功
*/
private boolean isSuccess(int productId) {
    int affectedCount = 0;
  	while (affectedCount == 0) {
      ProductStock product = excute("select * from product_stock where product_id=#{productId}");
      if (product.getNumber>0) {
          affectedCount = excute("update product_stock set number=number-1 where product_id=#{productId} and number=#{product.getNumber}");
      } else {
        return false;
      }
    }
  	return true;
  
  /*
  更简洁的写法
  int affectedCount = 
      excute("update product_stock set number=number-1 where product_id=#{productId} and number>0");
	return affectedCount!=0;*/
}
```
通过给商品库存加悲观锁，判断秒杀是否成功的伪代码:

```java
/**
* 该方法返回是否秒杀成功
* @param productId 产品ID、
* @return 是否秒杀成功
*/
private boolean isSuccess(int productId) {
    ProductStock product = excute("select * from product_stock where product_id=#{productId} for update");
  	
  	if (product.getNumber>0) {
      excute("update product_stock set number=number-1 where product_id=#{productId}");
      return true;
  	} else {
      return false;
  	}
}
```

#### 乐观锁与悲观锁的区别

乐观锁的思路一般是表中增加版本字段，更新时where语句中增加版本的判断，算是一种CAS（Compare And Swep）操作，商品库存场景中number起到了版本控制的作用（ `and number=#{product.getNumber}`）。悲观锁之所以是悲观，在于他认为外面的世界太复杂，所以一开始就对商品加上锁（`select ... for update`），后面可以安心的做判断和更新，因为这时候不会有别人更新这条商品库存。

#### MySQL乐观锁并不乐观

商品库存表中数量作为版本控制的特殊性，所以给商品库存加乐观锁可以简化为注释中更简洁的写法。在一般场景中我并不推荐用MySQL的乐观锁，因为他并不乐观：在`excute("update product_stock set number=number-1 where product_id=#{productId} and number=#{product.getNumber}")`执行之后，即使商品数量在更新前发生了变化，更新数量为0，MySQL也会给`product_stock`的`product_id`索引加锁；这里的while循环执行不会超过3次。乐观锁是希望在不加锁(不阻塞)的情况下实现更新，显然MySQL做不到。

## 1.2 分布式锁

网上有很多分布式锁的实现，这里讲一下基于redis的分布式锁的实现：

redis中有个命令`setNX`，是一种CAS操作，定义是

```java
	/**
	 * Set {@code value} for {@code key}, only if {@code key} does not exist.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/setnx">Redis Documentation: SETNX</a>
	 */
	Boolean setNX(byte[] key, byte[] value);
```

不同于一般的`set`命令直接覆盖原值，`setNx`在更新的时候会判断当前`key`是否存在，如果存在返回`false`，如果不存在设置`value`并返回`true`。下面的代码利用这个CAS操作写的乐观锁：

```java
/**
 * 获取锁
 *
 * @param key 锁id
 * @return 锁结果
 */
public boolean lock(String key) {
        try {
            while (true) {
                if (redisTemplate.opsForValue().setIfAbsent(key, "")) {
                    redisTemplate.expire(key, 5000, TimeUnit.MILLISECONDS);
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("get lock {} error", key, e);
        }
        return false;
    }
```

释放锁的方法很简单，删掉就可以

```java
/**
 * 释放锁
 *
 * @param key 锁id
 */
public void unlock(String key) {
        redisTemplate.delete(key);
}
```

## 1.3 存在的问题

在低并发下，上面的两种解决方案都没有什么问题，但在应对类似秒杀应用的高并发场景下，上面的解决方案存在的问题：

1. MySQL

   MySQL就不是为高并发而生的，强一致性才是它的追求。高并发下使用得考虑分库分表，甚至在应用层就得做分离。

2. 分布式缓存

   redis作为高性能的缓存系统，数据层并不存在太大问题。而对于应用层的每个节点来说，高并发下大量线程在运行获取锁的操作，每次其实只会有一个线程处于得到锁的状态；也就是说某一有很多线程运行着无意义的循环，浪费CPU时间。

#2. 我的方案

### 2.1 简介

对于单个系统实例而言，不要做过多无谓的循环，每次派一个线程代表和其它系统实例竞争这把锁，其它线程等待。简单的画个图和普通的分布式锁比较下：

//TODO

简而言之，单系统里的竞争同一把锁的线程排队，我把实现线程排队的锁叫本地锁；每个单系统派一个线程和其它系统竞争锁，各个系统竞争的叫分布式锁。

[github地址]: https://github.com/adamswanglin/wllock

### 2.2 本地锁实现

首先，有个静态变量`LOCK_MAP`，`key`值就是锁id，`value`是JUC包下的`ReentrantLock`，线程间的排队就是通过ReentrantLock实现的。

```java
private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();
```

贴上获取锁的代码

```java

    /**
     * 获取本地锁
     * @param key 锁id
     * @param tryMilliSeconds 最大等待时长
     * @return 锁结果
     */
	public LocalLockResult tryLock(String key, int tryMilliSeconds) {
        //1. 根据key值获取ReentrantLock
        LocalLockResult localLockResult = new LocalLockResult();
        ReentrantLock reentrantLock = LOCK_MAP.get(key);
        if (reentrantLock == null) {
            reentrantLock = new ReentrantLock(true);
            //没有key对应的ReentrantLock，写入新的ReentrantLock，并发场景下用了putIfAbsent
            ReentrantLock existed = LOCK_MAP.putIfAbsent(key, reentrantLock);
            if (existed != null) {
                reentrantLock = existed;
            }
        }
        localLockResult.setLock(reentrantLock);
        //如果当前排队数量超过阈值，服务降级返回获取锁失败
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
```

下面是释放本地锁的代码

```java
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
```

为防止LOCK_MAP里的key值一直加入，导致内存泄漏，在释放锁的时候会判断当前ReentrantLock是否有排队队列，如果没有说明当前并发量小，可以删掉；需要说明的这个操作并不是线程安全的，也就是说可能存在从MAP中删除ReentrantLock时，ReentrantLock中有新加入的线程在排队，不过这并不影响获取锁。

### 2.3 分布式锁实现

分布式锁就是基于redis的实现，不过增加了等待时长的概念，可以设置超过一定时间自动放弃加锁。

```java

/**
 * 获取分布式锁
 *
 * @param key             锁id
 * @param tryMilliSeconds 限定时长
 * @return 锁结果
 */
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
```

### 2.4 AOP的方式实现加锁

```java

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
```

其中tryLock是本地方法：

```java

    /**
     * 获取本地锁和分布式锁
     *
     * @param key          分布式锁key
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
```

### 2.5 使用方法

详见github中的example


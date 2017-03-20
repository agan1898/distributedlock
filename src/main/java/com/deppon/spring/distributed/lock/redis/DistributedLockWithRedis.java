package com.deppon.spring.distributed.lock.redis;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public abstract class DistributedLockWithRedis<I,O> implements DistributedLockWithRedisAPI<I,O>{
	// 串行化
	private static ReentrantLock reenTrantLock = new ReentrantLock();
	
	// 获取本地锁的最大时长
	private long maxGetLocalLockTime;

	// 获取分布式锁的最大时长
	private long maxGetDistributedLockTime;
	
	// 分布式锁的锁名
	private String distributedLockName;
	
	// 缓存数据自动控制的有效时长(单位为秒)
	private int maxLockExpiredTime;
	
	// 当前线程持锁时长
	private long maxHoldDistributedLockTime;
	
	// 最大重试抢锁次数
	private int maxRetryTimes;
	
	protected DistributedLockWithRedis(
			long maxGetLocalLockTime,
			long maxGetDistributedLockTime,
			long maxHoldDistributedLockTime,
			int maxRetryTimes,
			String distributedLockName){
		this.maxGetLocalLockTime = maxGetLocalLockTime;
		this.maxGetDistributedLockTime = maxGetDistributedLockTime;
		this.maxHoldDistributedLockTime = maxHoldDistributedLockTime;
		try{
			int middle = Integer.parseInt(String.valueOf(maxHoldDistributedLockTime))/1000;
			this.maxLockExpiredTime = (middle== 0?10:2*middle);
		}catch(Exception e){
			maxLockExpiredTime = 10;
		}
		this.maxRetryTimes = maxRetryTimes;
		this.distributedLockName = distributedLockName;
	}
	
	// setnx --- 当且仅当 key 不存在，将 key 的值设为 value ，并返回1；若给定的 key 已经存在，则 SETNX不做任何动作，并返回0。 
	// getSet -- 将给定 key 的值设为 value ，并返回 key 的旧值 (old value)，当 key存在但不是字符串类型时，返回一个错误，当key不存在时，返回nil。
	// 争抢锁
	private Boolean isGetDistributedLock(Jedis jedis, String distributedLockName) throws Exception{
			// 用于设置成功后的缓存处理时长
			long cacheProcessTime = System.currentTimeMillis() + maxHoldDistributedLockTime;
			
			// key为空时，争抢锁
			long isSetSuccessWhileKeyIsNil = jedis.setnx(distributedLockName, String.valueOf(cacheProcessTime));
			
			// setnx成功，则成功获取一把锁，并设置数据失效时间
			if (isSetSuccessWhileKeyIsNil == 1) {
				jedis.expire(distributedLockName, maxLockExpiredTime);
				return true;
			}

			// key不为空，setnx失败，说明锁被其他客户端保持，检查其是否已经超时
			String lastLockTimeBySomeThread = jedis.get(distributedLockName);
			
			// 如果获取key为空，则重走空key时的加锁流程
			if (lastLockTimeBySomeThread == null) {
				return false;
			}

			// 获取key不为空，则判断是否超时，若未超时，则循环重试
			if (Long.valueOf(lastLockTimeBySomeThread) > System.currentTimeMillis()) {
				return false;
			}

			// 若超时，则进行争抢加锁
			cacheProcessTime = System.currentTimeMillis() + maxHoldDistributedLockTime;
			String getOldIfSet = jedis.getSet(distributedLockName, String.valueOf(cacheProcessTime));

			// 判断加锁是否成功
			if (getOldIfSet != null && getOldIfSet.equals(lastLockTimeBySomeThread)) {
				return true;
			}

			// 若加锁失败，重头再来
			return false;
	}
	
	// 释放锁
	private void releaseLock(Jedis jedis, String lock) throws Exception {
			String lastLockTimeBySomeThread = jedis.get(lock);
			if (lastLockTimeBySomeThread == null) {
				return;
			}
			// 避免删除非自己获取得到的锁
			if (System.currentTimeMillis() < Long.valueOf(lastLockTimeBySomeThread)) {
				jedis.del(lock);
			}
	}
	

	// 执行任务
	private O executeTaskWithDistributedLock(I inPatameter){
		O outParameter = null;
		// 执行业务操作
		try {
			outParameter = executeTask(inPatameter);
		} catch (Exception e) {
			System.out.println("线程：" + Thread.currentThread().getName() + "===执行任务出现异常 ===");
			e.printStackTrace();
		}
		return outParameter;
	}
	
	
	// 获取分布式锁，获取到以后执行任务
	private O process(I inParameter,Pool<Jedis> jedisPool,Jedis jedis) {
		
		// 任务开始，初始化
		O outParameter = null;
		Boolean isSleeped = false;
		Integer retryTimes = 0;
		
		// 是否继续抢锁的判断条件
		Boolean isNotGetDistributedLock = true;
		Boolean isNotTimeOut = true;
		Boolean isNotMaxRetryTimes = true;
		
		// 循环等待拿锁(条件：是否抢到锁，是否超时，是否超过允许次数)
		long startTime = System.currentTimeMillis();
		while (isNotGetDistributedLock && isNotTimeOut && isNotMaxRetryTimes) {
			// 是否超时
			isNotTimeOut = ((System.currentTimeMillis() - startTime) <= maxGetDistributedLockTime);
			// 是否达到最大争抢次数
			isNotMaxRetryTimes = (retryTimes < maxRetryTimes);
			try {
				if(isSleeped){
					System.out.println(Thread.currentThread().getName()+"****已经睡眠过，又起来抢****");
				}
				
				// 抢锁
				isNotGetDistributedLock = this.isGetDistributedLock(jedis, distributedLockName);
				
				// 睡眠过后抢到了锁
				if(isSleeped && !isNotGetDistributedLock){
					isSleeped = false;
					System.out.println(Thread.currentThread().getName() + "****经过一段时间睡眠，再次抢锁，抢到了****");
				}
				
				// 获得锁了，开始执行业务逻辑
				if(!isNotGetDistributedLock){
					System.out.println("线程：" + Thread.currentThread().getName() + "===获得分布式锁===");
					outParameter = executeTaskWithDistributedLock(inParameter);
				}
				
				// 否则睡眠一段时间再去抢
				else{
					try {
						retryTimes++;
						System.out.println(Thread.currentThread().getName() + " 第   " + retryTimes + " 次未抢到锁");
						isSleeped = true;
						Thread.sleep(1);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		long endTime = System.currentTimeMillis();
		
		// 如果获得了分布式锁，先释放分布式锁
		if(!isNotGetDistributedLock){
			System.out.println("线程：" + Thread.currentThread().getName() + "===释放分布式锁===");
			try {
				// 释放锁
				this.releaseLock(jedis, distributedLockName);
			} catch (Exception e) {
				System.out.println("释放分布式锁出现异常");
				e.printStackTrace();
			} finally {
				// 释放redis连接
				returnResource(jedisPool,jedis);
			}
		}
		// 否则
		else{
			System.out.println("线程：" + Thread.currentThread().getName() + "===未获得分布式锁===");
		}
		
		if(isNotTimeOut){
			System.out.println(Thread.currentThread().getName()
					+ "任务设置的值:" 
					+  outParameter 
					+ "  " 
					+ "共消耗时长：" + (endTime - startTime) + "ms");
		}
		
		// 因为超时退出
		else{
			System.out.println(Thread.currentThread().getName()
					+ "任务超时，抢锁失败，当前值为:" 
					+  outParameter 
					+ "  " 
					+ "共消耗时长：" + (endTime - startTime) + "ms");
		}
		
		if(!isNotMaxRetryTimes){
			System.out.println(Thread.currentThread().getName()
					+ "任务次数超限，抢锁失败，当前值为:" 
					+  outParameter 
					+ "  " 
					+ "共重试次数：" + retryTimes + "次");
		}
		
		return outParameter;
	}
	
		
	@SuppressWarnings("finally")
	@Override
	public O start(I inPatameter,Pool<Jedis> jedisPool) {
		O outParameter = null;
		Boolean isGetLocalLock = false;
		Jedis jedis = getJedis(jedisPool);
		try {
			// 尝试去获取本地锁
			isGetLocalLock = reenTrantLock.tryLock(maxGetLocalLockTime, TimeUnit.MILLISECONDS);
			// 如果获得了本地锁
			if (isGetLocalLock) {
				System.out.println("线程：" + Thread.currentThread().getName() + "===获得本地锁===");
				// 尝试获取分布式锁,并在获取到分布式锁后执行业务操作
				outParameter = this.process(inPatameter,jedisPool,jedis);
			}
			// 否则
			else {
				System.out.println("线程：" + Thread.currentThread().getName() + "===未获得本地锁===");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 如果获得了本地锁,释放本地锁
			if (isGetLocalLock && reenTrantLock.isLocked()) {
				System.out.println("线程：" + Thread.currentThread().getName() + "===释放本地锁===");
				reenTrantLock.unlock();
			}
			// 否则
			else {
				System.out.println("线程：" + Thread.currentThread().getName() + "===未获得本地锁===");
			}
			return outParameter;
		}
	}
	
	private synchronized static Jedis getJedis(Pool<Jedis> jedisPool){
	    try {
	        if (jedisPool != null) {
	            Jedis resource = jedisPool.getResource();  
	            return resource;  
	        } else {
	            return null;  
	        }
	    } catch (Exception e) {
	  	  System.out.println("****从redis连接池获取连接异常****");
	        e.printStackTrace();  
	        return null;  
	    }
	}
	
	@SuppressWarnings("deprecation")
	private static void returnResource(Pool<Jedis> jedisPool,final Jedis jedis) {  
	    if (jedis != null) {  
	         jedisPool.returnResourceObject(jedis);  
	    }
	}
}

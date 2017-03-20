package com.deppon.spring.distributed.lock.redis;

import java.util.concurrent.CountDownLatch;

public class DistributedLockWithRedisDemo<I,O> extends DistributedLockWithRedis<I,O>{

	// 计数器
	protected static CountDownLatch count = new CountDownLatch(100000000);
	
	protected DistributedLockWithRedisDemo(
			long maxGetLocalLockTime,   // 根据并发线程数来配置,单位为毫秒
			long maxGetDistributedLockTime, // 根据应用数来配置,必须大于maxHoldDistributedLockTime，单位为毫秒
			long maxHoldDistributedLockTime,// 根据持锁时长来配置,单位为毫秒
			int maxRetryTimes,
			String distributedLockName) {
		super(maxGetLocalLockTime,
			maxGetDistributedLockTime, 
			maxHoldDistributedLockTime, 
			maxRetryTimes,
			distributedLockName);
	}
	

	
	@SuppressWarnings("unchecked")
	@Override
	public O executeTask(I inParam) throws Exception {
		System.out.println("线程：" + Thread.currentThread().getName() + "===正在执行任务===");
		count.countDown();
		// ---------
		System.out.println("线程：" + Thread.currentThread().getName() + "===执行任务结束===" + count.toString());
		return (O)count.toString();
	}
}

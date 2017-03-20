package com.deppon.spring.distributed.lock.zookeeper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;

public class DistributedLockWithZookeeperDemo<N,T> extends DistributedLockWithZookeeper<N,T>{
	// 计数器
	protected static CountDownLatch count = new CountDownLatch(1000);
	
	protected DistributedLockWithZookeeperDemo(CuratorFramework client, long maxGetLocalLockTime, long maxGetDistributedLockTime,
			TimeUnit getLocalLockTimeUnit, TimeUnit getDistributedLockTimeUnit, String path) {
		super(client, 
				maxGetLocalLockTime, 
				maxGetDistributedLockTime, 
				getLocalLockTimeUnit, 
				getDistributedLockTimeUnit, 
				path);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T executeTask(N inParam) {
		System.out.println("线程：" + Thread.currentThread().getName() + "===正在执行任务===");
		count.countDown();
		System.out.println("线程：" + Thread.currentThread().getName() + "===执行任务结束===" + count.toString());
		T o = (T)count.toString();
		return o;
	}
}

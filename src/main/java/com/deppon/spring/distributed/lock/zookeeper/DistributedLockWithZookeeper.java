package com.deppon.spring.distributed.lock.zookeeper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

public abstract class DistributedLockWithZookeeper<N,T> implements DistributedLockWithZookeeperAPI<N,T>{
	
	// 串行化
	private static ReentrantLock reenTrantLock = new ReentrantLock();
	
	private long maxGetLocalLockTime;
	
	private long maxGetDistributedLockTime;
	
	private TimeUnit getLocakLockTimeUnit;
	
	private TimeUnit getDistributedLockTimeUnit;
	
	private InterProcessMutex distributedLock; 
	
	protected DistributedLockWithZookeeper(
			CuratorFramework client,
			long maxGetLocalLockTime,
			long maxGetDistributedLockTime,
			TimeUnit getLocakLockTimeUnit,
			TimeUnit getDistributedLockTimeUnit,
			String path){
		this.maxGetLocalLockTime = maxGetLocalLockTime;
		this.maxGetDistributedLockTime = maxGetDistributedLockTime;
		this.getLocakLockTimeUnit = getLocakLockTimeUnit;
		this.getDistributedLockTimeUnit = getDistributedLockTimeUnit;
		distributedLock = new InterProcessMutex(client, path); 
	}
	
	private Boolean isGetDistributedLock(){
		Boolean isGetDistributedLock = false; 
		try {
			isGetDistributedLock = distributedLock.acquire(maxGetDistributedLockTime, getDistributedLockTimeUnit);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isGetDistributedLock;
	}
	
	private T executeTaskWithDistributedLock(N inPatameter){
		T outParameter = null;
		// 执行业务操作
		System.out.println("线程：" + Thread.currentThread().getName() + "===获得分布式锁===");
		try {
			outParameter = executeTask(inPatameter);
		} catch (Exception e) {
			System.out.println("线程：" + Thread.currentThread().getName() + "===执行任务出异常了===");
			e.printStackTrace();
		}
		return outParameter;
	}
	
	@SuppressWarnings("finally")
	@Override
	public T start(N inPatameter) {
		T outParameter = null;
		Boolean isGetLocalLock = false;
		Boolean isGetDistributedLock = false;
		try {
			// 尝试去获取本地锁
			isGetLocalLock = reenTrantLock.tryLock(maxGetLocalLockTime, getLocakLockTimeUnit);
			// 如果获得了本地锁
			if (isGetLocalLock) {
				// 尝试获取分布式锁
				isGetDistributedLock = this.isGetDistributedLock();
				// 如果获取到了分布式锁
				if (isGetDistributedLock) {
					// 执行业务操作
					outParameter = this.executeTaskWithDistributedLock(inPatameter);
				}
			}
			// 否则
			else {
				System.out.println("线程：" + Thread.currentThread().getName() + "===未获得本地锁===");
			}
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		} finally {
			// 如果获得了分布式锁，先释放分布式锁
			System.out.println("线程：" + Thread.currentThread().getName() + "===释放分布式锁===");
			if(isGetDistributedLock && distributedLock.isAcquiredInThisProcess()){
				try {
					distributedLock.release();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// 如果获得了本地锁,释放本地锁
			System.out.println("线程：" + Thread.currentThread().getName() + "===释放本地锁===");
			if (isGetLocalLock && reenTrantLock.isLocked()) {
				reenTrantLock.unlock();
			}
			
			// 否则
			else {
				System.out.println("线程：" + Thread.currentThread().getName() + "===未获得本地锁===");
			}
			return outParameter;
		}
	}
}

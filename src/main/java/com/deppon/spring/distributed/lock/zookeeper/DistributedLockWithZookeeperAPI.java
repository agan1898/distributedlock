package com.deppon.spring.distributed.lock.zookeeper;

public interface DistributedLockWithZookeeperAPI<N,T>{
	
	public T start(N inParam);
	
	public T executeTask(N inParam) throws Exception;
	
}
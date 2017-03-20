package com.deppon.spring.distributed.lock.redis;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public interface DistributedLockWithRedisAPI<I,O>{
	
	public O start(I inParam,Pool<Jedis> jedisPool);
	
	public O executeTask(I inParam) throws Exception;

}
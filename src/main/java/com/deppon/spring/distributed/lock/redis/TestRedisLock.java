package com.deppon.spring.distributed.lock.redis;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import com.deppon.spring.distributed.lock.redis.BuildDistributedLockWithRedisDB.RedisUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

public class TestRedisLock {
	// 总耗时
	private static AtomicLong totalTime = new AtomicLong(0);
	
	public static void main(String[] args) {
		
		// 任务数
		int produceTaskMaxNumber = 1*1000;		
		// 初始化线程池,模拟并发
		int corePoolSize = produceTaskMaxNumber;
		int maximumPoolSize = produceTaskMaxNumber;
		long keepAliveTime = 20;
		TimeUnit unit =TimeUnit.SECONDS;
		BlockingQueue<Runnable> workQueu = new ArrayBlockingQueue<Runnable>(produceTaskMaxNumber);
		RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardOldestPolicy();
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
				corePoolSize, 
				maximumPoolSize, 
				keepAliveTime, 
				unit,
				workQueu,
				handler);
		
		// 初始化序列值
		Jedis jedis = RedisUtils.getJedis();
		jedis.set("lock","0");
		RedisUtils.returnResource(jedis);
		
		// 创建任务
		for (int i = 1; i<= produceTaskMaxNumber; i++) {
			try {
				threadPool.execute(new DistributedLockWithRedisTask());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		long startTime = System.currentTimeMillis();
		// 不再提交新任务，等待执行的任务不受影响 
		threadPool.shutdown(); 
        try {
        	// 等待所有任务完成 
            boolean loop = true;  
            do {
                // 阻塞，直到线程池里所有任务结束
                loop = !threadPool.awaitTermination(1, TimeUnit.SECONDS);
            } while(loop);  
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }  
        long endTime = System.currentTimeMillis();
        totalTime.getAndAdd(endTime - startTime);	
        
		// 统计
        System.out.println("============ " + "任务执行完毕" + "  ===============");
		System.out.println("total        tasks:  "  +  produceTaskMaxNumber);
		System.out.println("concurrent threads:  "  +  maximumPoolSize);
		System.out.println("total elapsed time:  "  +  Double.parseDouble(totalTime.toString())/(1000) + "s");
		System.out.println("average       time:  "  +  Double.parseDouble(totalTime.toString())/(produceTaskMaxNumber) + "ms");
	}
	
	static class DistributedLockWithRedisTask implements Runnable {

		private static String hostAddress = "localhost";
		
		private static int maxTotal = 10;
		
		private static int maxIdle = 10;
		
		private static int minIdle = 10;
		
		private static Pool<Jedis> jedisPool;
		
		// redis-pool-config
		static {
			GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
			poolConfig.setMaxTotal(maxTotal);
			poolConfig.setMaxIdle(maxIdle);
			poolConfig.setMinIdle(minIdle);
			poolConfig.setTestOnBorrow(true);
			poolConfig.setTestOnReturn(true);
			jedisPool = new JedisPool(poolConfig,hostAddress);
		}
		
		@Override
		public void run() {
			new DistributedLockWithRedisDemo<String, String>(100*1000, 100*1000, 100*1000, 1000,"lock").start("sequenceData", jedisPool);
		}
	}
}

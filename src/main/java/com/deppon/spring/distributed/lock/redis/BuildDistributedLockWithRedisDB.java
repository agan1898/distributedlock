package com.deppon.spring.distributed.lock.redis;

import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

/**
 * 
 * @author ganxj
 *
 */
public class BuildDistributedLockWithRedisDB {
	
	// 锁名
	private static final String lockName = "lock";
	
	// 序列名
	private static final String sequenceName = "sequence";
	
	// 数据失效时间
	private static final int expired = 5;

	// 当前线程处理当次缓存操作所需时间，(预估值)
	private static final int time = 10 * 1000;
	
	// 步长
	private static final BigInteger stepLength = new BigInteger("1");
	
	// 总耗时
	private static AtomicLong totalTime = new AtomicLong(0);

	// 串行化
	private static ReentrantLock reenTrantLock = new ReentrantLock();
	
	// 抢锁超时时间
	private static long timerOut = expired*500;
	
	// 数据
	private static ThreadLocal<String> threadLocal = new ThreadLocal<String>();
	
	public static void main(String[] args) {
		// 任务数
		int produceTaskMaxNumber = 1000;		
		
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
		jedis.set(sequenceName,"0");
		RedisUtils.returnResource(jedis);
		
		// 创建任务
		for (int i = 1; i<= produceTaskMaxNumber; i++) {
			try {
				threadPool.execute(new BuildDistributedLockWithRedisDB().new RedisLockTask("任务" + i));
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
	
	// setnx --- 当且仅当 key 不存在，将 key 的值设为 value ，并返回1；若给定的 key 已经存在，则 SETNX不做任何动作，并返回0。 
	// getSet -- 将给定 key 的值设为 value ，并返回 key 的旧值 (old value)，当 key存在但不是字符串类型时，返回一个错误，当key不存在时，返回nil。
	// 争抢锁
	public static boolean acquireLock(Jedis jedis, String lock) throws Exception {
		try {

			// 用于设置成功后的缓存处理时长
			long cacheProcessTime = System.currentTimeMillis() + time;
			
			// key为空时，争抢锁
			long isSetSuccessWhileKeyIsNil = jedis.setnx(lock, String.valueOf(cacheProcessTime));
			
			// SETNX成功，则成功获取一个锁，并设置数据失效时间
			if (isSetSuccessWhileKeyIsNil == 1) {
				jedis.expire(lock, expired);
				return true;
			}

			// key不为空，SETNX失败，说明锁被其他客户端保持，检查其是否已经超时
			String lastLockTimeBySomeThread = jedis.get(lock);

			// 如果获取key为空，则重走空key时的加锁流程
			if (lastLockTimeBySomeThread == null) {
				return false;
			}

			// 获取key不为空，则判断是否超时，若未超时，则循环重试
			if (Long.valueOf(lastLockTimeBySomeThread) > System.currentTimeMillis()) {
				return false;
			}

			// 若超时，则进行争抢加锁
			String getOldIfSet = jedis.getSet(lock, String.valueOf(cacheProcessTime));

			// 判断加锁是否成功
			if (getOldIfSet != null && getOldIfSet.equals(lastLockTimeBySomeThread)) {
				return true;
			}

			// 若加锁失败，重头再来
			return false;

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	

	// 释放锁
	public static void releaseLock(Jedis jedis, String lock) throws Exception {
		
		try {
			
			String lastLockTimeBySomeThread = jedis.get(lock);
			if (lastLockTimeBySomeThread == null) {
				return;
			}
			
			// 避免删除非自己获取得到的锁
			if (System.currentTimeMillis() < Long.valueOf(lastLockTimeBySomeThread)) {
				jedis.del(lock);
			}
			
		} catch (Exception e) {
			System.out.println("释放锁的时候出现了异常");
			throw e;
		}
		
	}
	
	
	static class RedisUtils{
		
		private static String hostAddress = "localhost";
		
		private static int maxTotal = 1000;
		
		private static int maxIdle = 1000;
		
		private static int minIdle = 1000;
		
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
		
		public synchronized static Jedis getJedis(){
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
       public static void returnResource(final Jedis jedis) {  
           if (jedis != null) {  
                jedisPool.returnResourceObject(jedis);  
           }
       }
       
	}

	
	class RedisLockTask implements Runnable {
		
		@SuppressWarnings("unused")
		private RedisLockTask(){
		}
		
		public RedisLockTask(String taskName) {
		}
		
		@Override
		public void run() {
			RedisDistributedTask();
		}
		
	}
	
	static void RedisDistributedTask(){
		
		String sequenceData = null;
		Boolean isGetLocalLock = false;
		
		try {
			// 尝试去获取本地锁
			isGetLocalLock = reenTrantLock.tryLock(60, TimeUnit.SECONDS);
			System.out.println("线程：" + Thread.currentThread().getName() + "===尝试获取可重入锁，状态:" + isGetLocalLock);
			
			// 如果获得了本地锁
			if (isGetLocalLock){
				sequenceData = RedisLockTaskExecutor.execute();
			}
			
			// 否则
			else {
				System.out.println("线程：" + Thread.currentThread().getName() + "===获取可重入锁失败===");
			}
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		} finally {
			// 如果获得了本地锁
			if(isGetLocalLock && reenTrantLock.isLocked()){
				// 存入数据
				threadLocal.set(sequenceData);
				System.out.println("线程：" + Thread.currentThread().getName() + "===数据===" + threadLocal.get());
				
				// 释放本地锁
				reenTrantLock.unlock();
			}
			// 否则
			else {
				System.out.println("线程：" + Thread.currentThread().getName() + "====未获得锁");
			}
		}
	}
	
	private static String process(Jedis jedis,String sequenceName){
		String sequeceData = new BigInteger(jedis.get(sequenceName)).add(stepLength).toString();
		jedis.set(sequenceName, sequeceData);
		return sequeceData;
	}
	
	static class RedisLockTaskExecutor{

		
		private static String execute(){
			// 任务开始
			long startTime = System.currentTimeMillis();
			// Timer-Bean
			class TimerBean{
				private Boolean isNotTimeOut = true;

				public Boolean getIsNotTimeOut() {
					return isNotTimeOut;
				}

				public void setIsTimeOut(Boolean isTimeOut) {
					this.isNotTimeOut = isTimeOut;
				}
			}
			
			// 定时任务
			class LockTimerTask extends TimerTask{
				private TimerBean timerBean;
				
				@SuppressWarnings("unused")
				private LockTimerTask(){
				}
				
		        public LockTimerTask(TimerBean timerBean) {
		        	this.timerBean = timerBean;
		        }
		         
				@Override
				public void run() {
					timerBean.setIsTimeOut(false);
				}
			}
			Timer timer = new Timer();
			TimerBean timerBean = new TimerBean();
			TimerTask timeTask = new LockTimerTask(timerBean);
		    timer.schedule(timeTask, timerOut);
		    
			// 获取redis连接
			Jedis jedis = RedisUtils.getJedis();
			String sequeceData = null;
			Boolean isSleeped = false;
			Boolean isGetDistributedLock = false;
			Integer retryTimes = 0;
			
			// 循环等待拿锁(条件：是否抢到锁，是否超时，是否超过允许次数)
			while (!isGetDistributedLock && timerBean.getIsNotTimeOut() && retryTimes<2) {
				try {
					if(isSleeped){
						System.out.println(Thread.currentThread().getName()+"****已经睡眠过，又起来抢****");
					}
					
					// 抢锁
					isGetDistributedLock = BuildDistributedLockWithRedisDB.acquireLock(jedis, lockName);
					
					// 睡眠过后抢到了锁
					if(isSleeped && isGetDistributedLock){
						isSleeped = false;
						System.out.println(Thread.currentThread().getName() + "****经过一段时间睡眠，再次抢锁，抢到了****");
					}
					
					// 获得锁了，开始执行业务逻辑
					if(isGetDistributedLock){
						sequeceData = process(jedis, sequenceName);
					}
					
					// 否则睡眠一段时间再去抢
					else{
						try {
							retryTimes++;
							System.out.println(Thread.currentThread().getName() + " 第   " + retryTimes + " 次未抢到锁");
							isSleeped = true;
							Thread.sleep(expired*500);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					try {
						System.out.println( Thread.currentThread().getName()
								+ "\r\n" + "***抢锁遇到异常了"
								+ "\r\n" + "***开始了睡眠 ^^^^^^^*********^^^^^!!!!!");
						isSleeped = true;
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					System.out.println("睡了一觉，再起来抢");
				}
				
			}
			long endTime = System.currentTimeMillis();
			
			if(timerBean.getIsNotTimeOut())
			{
				System.out.println(Thread.currentThread().getName()
						+ "任务设置的值:" 
						+  sequeceData 
						+ "  " 
						+ "共消耗时长：" + (endTime - startTime) + "ms");
			}
			
			// 因为超时退出
			else
			{
				System.out.println(Thread.currentThread().getName()
						+ "任务超时，抢锁失败，当前值为:" 
						+  sequeceData 
						+ "  " 
						+ "共消耗时长：" + (endTime - startTime) + "ms");
			}
			
			try {
				// 释放锁
				BuildDistributedLockWithRedisDB.releaseLock(jedis, lockName);
			} catch (Exception e) {
				System.out.println("释放锁的时候出异常了：" + e.getCause());
			}finally{
				// 释放redis连接
				RedisUtils.returnResource(jedis);
			}
			return sequeceData;
		}
		
	}

}
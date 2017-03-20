package com.deppon.spring.distributed.lock.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class TestZookeeperLock {
	
	// 总耗时
	private static AtomicLong totalTime = new AtomicLong(0);
	
	public static void main(String[] args) throws InterruptedException {
		String zookeeperConnectionString = "localhost:2181";
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
		client.start();

		// 任务数
		int produceTaskMaxNumber = 1000;
		// 初始化线程池,模拟并发
		int corePoolSize = produceTaskMaxNumber;
		int maximumPoolSize = produceTaskMaxNumber;
		long keepAliveTime = 20;
		TimeUnit unit = TimeUnit.SECONDS;
		BlockingQueue<Runnable> workQueu = new ArrayBlockingQueue<Runnable>(produceTaskMaxNumber);
		RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardOldestPolicy();
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
				workQueu, handler);

		// 创建任务
        long startTime = System.currentTimeMillis();
		for (int i = 1; i <= produceTaskMaxNumber; i++) {
			try {
//				if(i !=produceTaskMaxNumber && i%100==0){
//					Thread.sleep(20*1000);
//				}
				threadPool.execute(new TestZookeeperLock().new DistributedLockWithZookeeperTask(client));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
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
	
	
	class DistributedLockWithZookeeperTask implements Runnable {

		private CuratorFramework client;

		@SuppressWarnings("unused")
		private DistributedLockWithZookeeperTask() {
		}

		public DistributedLockWithZookeeperTask(CuratorFramework client) {
			this.client = client;
		}

		@Override
		public void run() {
			new DistributedLockWithZookeeperDemo<String,String>(
					client,
					500,
					5,
					TimeUnit.SECONDS,
					TimeUnit.SECONDS,
					"/global_path").start("123");
		}
	}
}
package com.deppon.spring.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootApplication
//@ImportResource({ "classpath:ActiveMQQQ.xml" })
@ImportResource({ "classpath:spring-mybatis.xml" })
@ComponentScan(basePackages = { "com.deppon" })
public class SpringApplicationBoot {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringApplicationBoot.class);
	
	@Bean
	StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
		return new StringRedisTemplate(connectionFactory);
	}
	
	public static void main(String[] args) throws InterruptedException {
		
		ApplicationContext ctx = SpringApplication.run(SpringApplicationBoot.class, args);
		
		System.out.println(args);
		
		// test-redis
		StringRedisTemplate template = ctx.getBean(StringRedisTemplate.class);
		template.opsForValue().set("123", "123");
		template.opsForValue().set("456", "456");
		LOGGER.info("---"+	template.opsForValue().get("123"));
		LOGGER.info("---"+	template.getConnectionFactory().getConnection().getNativeConnection());
		LOGGER.info("Sending message...");
		template.convertAndSend("AAAT", "Hello from Redis!");
		template.convertAndSend("chat", "Hello from Redis!");
		
		
		// test-mysql-database
	}
}

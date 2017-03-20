package com.deppon.spring.datasources;

import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
class MultiSqlSessionFactoryUtil implements ApplicationListener<ContextRefreshedEvent>{

    private Map<Object, SqlSessionFactory> masterSqlSessionFactorys;
	private Map<Object, SqlSessionFactory> slaveSqlSessionFactorys;
	
    public Map<Object, SqlSessionFactory> getMasterSqlSessionFactorys() {
		return masterSqlSessionFactorys;
	}

	public void setMasterSqlSessionFactorys(Map<Object, SqlSessionFactory> masterSqlSessionFactorys) {
		this.masterSqlSessionFactorys = masterSqlSessionFactorys;
	}

    
	public Map<Object, SqlSessionFactory> getSlaveSqlSessionFactorys() {
		return slaveSqlSessionFactorys;
	}

	public void setSlaveSqlSessionFactorys(Map<Object, SqlSessionFactory> slaveSqlSessionFactorys) {
		this.slaveSqlSessionFactorys = slaveSqlSessionFactorys;
	}

	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if(event.getApplicationContext().getParent() == null){
			ApplicationContext applicationContext = event.getApplicationContext();
			
		}
	}
}

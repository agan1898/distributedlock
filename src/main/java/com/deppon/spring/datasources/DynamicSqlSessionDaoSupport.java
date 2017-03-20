package com.deppon.spring.datasources;

import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * @Description 动态数据源
 * @author ganxiaojian
 *
 */
public class DynamicSqlSessionDaoSupport extends SqlSessionDaoSupport implements ApplicationContextAware {
 
    private ApplicationContext applicationContext;
    private static final String masterSuffix="_datasource_master";
    private static final String slaveSuffix="_datasource_slave";

    private Map<Object, SqlSessionFactory> masterSqlSessionFactorys;
    private Map<Object, SqlSessionFactory> slaveSqlSessionFactorys;
    private SqlSession sqlSession;
    private String factoryName;
    
    @Override
    public final SqlSession getSqlSession() {
    	
    	// 从主库中获取
        SqlSessionFactory sqlSessionFactory = masterSqlSessionFactorys.get(factoryName);
        
        // 如果主库不可用，则切换到从库
        if(sqlSessionFactory == null){
        	sqlSessionFactory = slaveSqlSessionFactorys.get(factoryName);
        }
        
        // 如果主从库均不可用，则切换到默认数据库(容灾数据库)
        if(sqlSessionFactory == null){
            sqlSessionFactory = (SqlSessionFactory) applicationContext.getBean(factoryName);
        }
        
        // 设置
        setSqlSessionFactory(sqlSessionFactory);
        this.sqlSession = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        
        return this.sqlSession;
        
    }
    
    /**
     * @description 根据数据源工厂名获取对应session
     * @param factoryName
     * @return
     */
    protected final SqlSession getSqlSession(String factoryName){
    	this.factoryName=factoryName;
    	return this.getSqlSession();
    }
    
    @Override
    protected void checkDaoConfig() {
        Assert.notNull(getSqlSession(), "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required");
    }
 
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
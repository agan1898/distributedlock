package com.deppon.spring.database;

import javax.annotation.Resource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.support.SqlSessionDaoSupport;

public class BaseDao extends SqlSessionDaoSupport{
	
    @Resource(name="sqlSessionFactory")
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory){          
        super.setSqlSessionFactory(sqlSessionFactory);  
    }
    
}
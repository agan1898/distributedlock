package com.deppon.spring.database;

import java.util.List;

import org.springframework.stereotype.Repository;


@Repository
public class LoadDao extends BaseDao{
	private static final String NAMESPACE=LoadDao.class.getName();
	
	public List<LoadEntity> findLoadInfo(){
		return this.getSqlSession().selectList(NAMESPACE+".findLoadInfo");
	}
}

package com.deppon.spring.database;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/datasource")
public class TestDataSourceController {
	
	@Autowired
	private LoadDao loadDao;
	
	@RequestMapping(value = "/queryLoadInfo")
	public List<LoadEntity> queryLoadInfo(){
		return loadDao.findLoadInfo();
	}
}

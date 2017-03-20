//package com.deppon.spring.test.activemq;
//
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.boot.test.SpringApplicationConfiguration;
//import org.springframework.context.annotation.ImportResource;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockServletContext;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.test.context.web.WebAppConfiguration;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import com.deppon.spring.activemq.ActivemqController;
//
//@RunWith(SpringJUnit4ClassRunner.class)  
//@SpringApplicationConfiguration(classes = MockServletContext.class)  
//@ImportResource({ "classpath:ActiveMQ.xml" })
//@WebAppConfiguration  
//public class GreetingControllerTest {
//    private MockMvc mvc;  
//  
//    @Before  
//    public void setUp() {
//        mvc = MockMvcBuilders.standaloneSetup(new ActivemqController()).build();  
//    }  
//  
//    @Test  
//    public void getHello() throws Exception {
//    	String expectedResult = "";
//    	String requestURI = "/activemq/queueSender";
//    	
//        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/activemq/queueSender").accept(MediaType.APPLICATION_JSON)).andReturn();  
//        int status = mvcResult.getResponse().getStatus();  
//        String content = mvcResult.getResponse().getContentAsString(); 
//        
//        System.out.println("-------"+content);
//        Assert.assertTrue("错误，正确的返回值为200", status == 200);  
//        Assert.assertFalse("错误，正确的返回值为200", status != 200);  
//        Assert.assertTrue("数据一致", expectedResult.equals(content));  
//        Assert.assertFalse("数据不一致", !expectedResult.equals(content));  
//    }  
//}  

//package com.deppon.spring.activemq;
//
//import javax.annotation.Resource;
//
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.ResponseBody;
// 
///**
// * @作者 Goofy
// * @邮件 252878950@qq.com
// * @日期 2014-4-1上午10:54:11
// * @描述 测试 
// */
//@Controller
//@RequestMapping("/activemq")
//public class ActivemqController {
//     
//    @Resource 
//    QueueSender queueSender;
//    
//    @Resource 
//    TopicSender topicSender;
//     
//    /**
//     * 发送消息到队列
//     * @param message
//     * @return String
//     */
//    @ResponseBody
//    @RequestMapping("/queueSender")
//    public String queueSender(@RequestParam("message")String message){
//        String opt="";
//        try {
//        	System.out.println("----message----");
//            queueSender.send("test.queue", message);
//            opt="suc";
//        } catch (Exception e) {
//            opt=e.getCause().toString();
//        }
//        return opt;
//    }
//     
//    /**
//     * 发送消息到主题
//     * @param message
//     * @return String
//     */
//    @ResponseBody
//    @RequestMapping("/topicSender")
//    public String topicSender(@RequestParam("message")String message){
//        String opt="";
//        try {
//            topicSender.send("test.topic", message);
//            opt="suc";
//        } catch (Exception e) {
//            opt=e.getCause().toString();
//        }
//        return opt;
//    }
//     
//}
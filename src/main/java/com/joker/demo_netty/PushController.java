package com.joker.demo_netty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0.0
 * @ClassName PushController.java
 * @Package com.joker.demo_netty
 * @Author Joker
 * @Description 主要为了测试 如何其他业务可以套用
 * @CreateTime 2021年02月23日 15:36:00
 */
@RestController
@RequestMapping("/push")
public class PushController {

    @Autowired
    private PushService pushService;

    /**
     * 推送给所有用户
     * @param msg
     */
    @PostMapping("/pushAll")
    public void pushToAll(@RequestParam("msg") String msg){
        pushService.pushMsgToAll(msg);
    }
    /**
     * 推送给指定用户
     * @param userId
     * @param msg
     */
    @PostMapping("/pushOne")
    public void pushMsgToOne(@RequestParam("userId") String userId,@RequestParam("msg") String msg){
        pushService.pushMsgToOne(userId,msg);
    }

}

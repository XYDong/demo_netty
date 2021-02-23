package com.joker.demo_netty;

/**
 * @version 1.0.0
 * @ClassName PushService.java
 * @Package com.joker.demo_netty
 * @Author Joker
 * @Description 具体消息推送的接口
 * @CreateTime 2021年02月23日 15:35:00
 */
public interface PushService {
    /**
     * 推送给指定用户
     * @param userId
     * @param msg
     */
    void pushMsgToOne(String userId,String msg);

    /**
     * 推送给所有用户
     * @param msg
     */
    void pushMsgToAll(String msg);
}

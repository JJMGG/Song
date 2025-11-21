package com.iteima.mysong.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.iteima.mysong.config.GetTokenSeesionConfig;
import com.iteima.mysong.config.SpringContextHolder;
import com.iteima.mysong.main.service.impl.SongListServiceImpl;
import com.iteima.mysong.pojo.entity.Message;
import jakarta.websocket.*;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws", configurator = GetTokenSeesionConfig.class)
@Component
public class WsContrller {
   private static final Map<String,Session> onlinUsers=new ConcurrentHashMap<>();

    private static SongListServiceImpl songListService;

    static {
        // 静态初始化时从 Spring 容器获取 Bean
        songListService = SpringContextHolder.getBean(SongListServiceImpl.class);
    }
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {

        System.out.println("first");
        System.out.println("连接打开");
        String userId = (String) session.getUserProperties().get("userId"); //从握手请求中获取token

        System.out.println(userId);
        System.out.println(session);
        onlinUsers.put(userId,session);

//        sendMessage("服务器收到你的消息：" + "你好",userId);
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("second");
        System.out.println("接收到消息：" + message);

        // 使用CompletableFuture异步处理
        CompletableFuture.runAsync(() -> {
            processMessage(message);
        }).exceptionally(e -> {
            System.err.println("消息处理异常: " + e.getMessage());
            return null;
        });
    }

    private void processMessage(String message) {
        try {
            String senderId = message.split("-")[0];
            String receiverId = message.split("-")[1].split("@")[0];
            LocalDateTime time = LocalDateTime.parse(message.split("@")[1]);

//            System.out.println(senderId + " " + receiverId + " " + time);

            Message messageTemp = songListService.findMessage(senderId, receiverId, time);
            String json;
            if (messageTemp == null) {
                json = "{\"error\":\"消息不存在\"}";
                System.out.println("数据库返回的数据为空");
            } else {
                json = songListService.tranToMessageVo(messageTemp);
            }

            // 异步发送消息
            sendMessageAsync(receiverId, json);

        } catch (Exception e) {
            System.err.println("处理消息异常: " + e.getMessage());
        }
    }

    private void sendMessageAsync(String receiverId, String message) {
        System.out.println("会话id"+receiverId);
        Session session = onlinUsers.get(receiverId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(message);
                }
                System.out.println("消息发送成功给用户: " + receiverId);
            } catch (IOException e) {
                System.err.println("发送消息失败，用户可能已离线: " + receiverId);
                // 移除已关闭的会话
                onlinUsers.remove(receiverId, session);
            }
        } else {
            System.out.println("用户不在线或会话已关闭: " + receiverId);
            if (session != null) {
                onlinUsers.remove(receiverId, session);
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("third");
        String userId = (String) session.getUserProperties().get("userId"); //从握手请求中获取token
        System.out.println("连接关闭: " + userId + ", 原因: " + closeReason.getReasonPhrase());
        // 清理资源
//        onlinUsers.values().removeIf(s -> s.getId().equals(userId));
    }
    // 主动向客户端发送消息
    public void sendMessage(String message,String account) {
        try {
            // 使用 Session 的 getBasicRemote() 方法发送文本消息
            Session session = onlinUsers.get(account);
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 可以定义一个方法来在任何地方主动发送消息给客户端

}






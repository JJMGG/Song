package com.iteima.mysong.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private int messageId;              //消息id
    private String messageSenderId;     //消息发送者id
    private int messageSendCommentId;   //评论id
    private String messageAvatar;      //头像
    private String messageSenderContent;  //消息内容
    private LocalDateTime messageTime;   //时间
    private String messageIsread;   //是否已读
    private String messageType;     //消息类型
    private int messageReplyerCommentId;   //消息回复评论id
    private String messageReplyerId;       //消息回复者id
    private String messageReplyerContent;   //消息回复者内容
    private String messageIsyou;//      是否给你
}

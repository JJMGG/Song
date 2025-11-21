package com.iteima.mysong.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private int messageId;
    private String messageSenderId;
    private int messageSendCommentId;
    private String messageAvatar;
    private String messageSenderContent;
    private LocalDateTime messageTime;
    private String messageIsread;
    private String messageType;
    private int messageReplyerCommentId;
    private String messageReplyerId;
    private String messageReplyerContent;
    private String messageIsyou;
}

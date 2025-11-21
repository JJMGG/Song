package com.iteima.mysong.pojo.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageVo implements Serializable {
    private int messageId;
    private String messageSenderId;
    private int messageSendCommentId;
    private String messageAvatar;
    private String messageSenderContent;
    private LocalDateTime messageTime;
    private boolean messageIsread;
    private String messageType;
    private String time;
    private ReplyToVo replyTo;
}

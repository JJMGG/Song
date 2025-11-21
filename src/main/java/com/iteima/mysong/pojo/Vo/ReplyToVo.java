package com.iteima.mysong.pojo.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplyToVo implements Serializable {
    private int messageReplyerCommentId;
    private String messageReplyerId;
    private String messageReplyerContent;
    private boolean messageIsyou;
}

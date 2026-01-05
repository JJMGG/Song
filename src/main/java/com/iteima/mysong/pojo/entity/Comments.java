package com.iteima.mysong.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comments {
    private int commId;     //评论id
    private String commDetails;  //评论内容
    private LocalDateTime commTime;   //评论时间
    private int commUserid;     //评论的用户
    private int commType;      //评论的类型
    private int commTargetid;     //评论对象
    private int commFather;     //评论的父级评论
    private int commReplyid;    //评论的回复id
    private int singerId;     //歌手id

}

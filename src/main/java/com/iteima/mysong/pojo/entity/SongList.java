package com.iteima.mysong.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongList {
    private int listId;   //歌单id
    private String  listTitle;  //歌单标题
    private String  listImg; //歌单图片
    private int  listUserid; //歌单作者
    private LocalDate listTime;//歌单创建时间
    private String listDetails;//歌单介绍
    private String listType;//歌单类型
    private Long listPlaynum;//歌单点击次数
}

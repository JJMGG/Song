package com.iteima.mysong.pojo.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListVo implements Serializable {
    //主界面的歌单展示
    private int listId;
    private String listTitle; //标题
    private String listImg;  //图片
}

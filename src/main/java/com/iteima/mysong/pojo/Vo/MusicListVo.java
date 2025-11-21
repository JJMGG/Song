package com.iteima.mysong.pojo.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicListVo implements Serializable {
    private int listId;
    private String listImg;
    private String listTitle;
}

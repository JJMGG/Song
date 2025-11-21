package com.iteima.mysong.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectDto implements Serializable {//歌单收藏
    private int userId;
    private int listId;
}

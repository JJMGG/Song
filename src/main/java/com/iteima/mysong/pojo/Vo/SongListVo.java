package com.iteima.mysong.pojo.Vo;

import com.iteima.mysong.pojo.entity.Songs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongListVo implements Serializable {
    private int listId;
    private String  listTitle;
    private String  listImg;
    private int  listUserid;
    private LocalDate listTime;
    private String listDetails;
    private List<String> listType;
    private Long listPlaynum;
    private List<SongVo> songs;
    private boolean Iscollect;
}

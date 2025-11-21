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
    private int listId;
    private String  listTitle;
    private String  listImg;
    private int  listUserid;
    private LocalDate listTime;
    private String listDetails;
    private String listType;
    private Long listPlaynum;
}

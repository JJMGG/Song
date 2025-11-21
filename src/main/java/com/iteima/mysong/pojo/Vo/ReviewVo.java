package com.iteima.mysong.pojo.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewVo {

    private String reviewSongName;
    private int reviewSinger;
    private String reviewFilepath;
    private int reviewSongAlbum;
    private String reviewLyc;
    private String reviewImg;
    private String reviewType;
    private String reviewTime;
}

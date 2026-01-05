package com.iteima.mysong.pojo.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewVo {

    private String reviewSongName;  //歌名
    private int reviewSinger;       //歌手名
    private String reviewFilepath;  //歌曲路劲
    private int reviewSongAlbum;    //歌曲专辑
    private String reviewLyc;       //歌曲歌词
    private String reviewImg;       //歌曲图片
    private String reviewType;       //歌曲类型
    private String reviewTime;      //上传时间
}

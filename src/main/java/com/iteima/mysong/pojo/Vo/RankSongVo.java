package com.iteima.mysong.pojo.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankSongVo implements Serializable {
    private Long songId;  //歌id

    private String songName; //歌名

    private Long songSinger; //歌手id

    private  String songFilepath; //歌曲路径

    private Long songAlbum; //专辑id

    private String songLyc; //歌词

    private String songImg; //歌曲图片

    private Long songNumber; //歌曲听歌次数

    private String songType; //歌曲类型

    private Integer songTime;
}

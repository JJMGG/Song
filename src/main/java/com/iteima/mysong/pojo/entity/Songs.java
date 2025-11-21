package com.iteima.mysong.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Songs {

    private Long songId;  //歌id

    private String songName; //歌名

    private Long songSinger; //歌手id

    private  String songFilepath; //歌曲路径

    private Long songAlbum; //专辑id

    private String songLyc; //歌词

    private String songImg; //歌曲图片

    private Long songNumber; //歌曲听歌次数

    private String songType; //歌曲类型

    private Integer songTime;//歌曲时长
}

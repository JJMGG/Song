package com.iteima.mysong.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Table;
import javax.persistence.Id;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "review")  // 指定数据库表名
public class Review {
    @Id
    private int reviewId;  //审核id

    private String reviewSongName;  //审核的歌名
    private int reviewSinger;      //歌手名
    private String reviewFilepath;  //歌曲路径
    private int reviewSongAlbum;        //歌曲专辑
    private String reviewLyc;   //歌词
    private String reviewImg; //图片
    private String reviewType; //类型
    private String reviewTime;  //审核时间
    private String reviewStatus;   //审核状态  0:待审核  1:同意 2:拒绝

}

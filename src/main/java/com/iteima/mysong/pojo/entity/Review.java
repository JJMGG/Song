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
    private int reviewId;

    private String reviewSongName;
    private int reviewSinger;
    private String reviewFilepath;
    private int reviewSongAlbum;
    private String reviewLyc;
    private String reviewImg;
    private String reviewType;
    private String reviewTime;
    private String reviewStatus;

}

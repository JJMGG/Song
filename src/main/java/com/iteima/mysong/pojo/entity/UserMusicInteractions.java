package com.iteima.mysong.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMusicInteractions {
    /**
     * 交互ID
     */
    private Long interactionId;

    /**
     * 用户id
     */
    private Integer	userId;

    /**
     * 歌曲id
     */
    private Integer	musicId;

    /**
     * 歌单id
     */
    private Integer	musicListId;

    /**
     * 歌手id
     */
    private Integer	singerId;

    /**
     * 搜索内容
     */
    private String	searchContent;

    /**
     * 交互类型
     */
    private String	actionType;

    /**
     * 行为权重
     */
    private String	actionValue;

    /**
     * 播放时间
     */
    private Integer	playDuration;

    /**
     * 是否点赞
     */
    private Integer	isLiked;

    /**
     * 是否收藏
     */
    private Integer	isCollected;

    /**
     * 交互时间
     */
    private LocalDateTime interactionTime;
}

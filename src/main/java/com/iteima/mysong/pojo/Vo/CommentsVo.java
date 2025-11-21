package com.iteima.mysong.pojo.Vo;

import com.iteima.mysong.pojo.entity.Comments;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="comments")
public class CommentsVo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int commId;
    private String commDetails;
    private LocalDateTime commTime;
    private int commUserid;
    private int commType;
    private int commTargetid;
    private int commFather;
    private int commReplyid;

    @Transient
    private List<CommentsVo> children;


    @Transient
    private  String commUserName;

    @Transient String commHeadShot;

    @Transient
    private  String commReplyName;
}

package com.iteima.mysong.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comments {
    private int commId;
    private String commDetails;
    private LocalDateTime commTime;
    private int commUserid;
    private int commType;
    private int commTargetid;
    private int commFather;
    private int commReplyid;
    private int singerId;

}

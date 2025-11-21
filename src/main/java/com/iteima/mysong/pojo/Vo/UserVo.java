package com.iteima.mysong.pojo.Vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class UserVo implements Serializable {
    private Integer userId; //用户Id

    private String userName; //用户名

    private String account;//账号

    private String headshot;//头像

    private String email;//邮箱

    private String sex; //性别

    private String briefIntroduction;//简介

    private String birthday;//生日

    private String region;//地区

    private String password;//密码

    private String token;//jwt令牌
}

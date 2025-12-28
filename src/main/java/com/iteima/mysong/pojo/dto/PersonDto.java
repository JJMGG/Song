package com.iteima.mysong.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonDto implements Serializable {
    private int userId;
    private String userName;
    private String email;
    private String birthday;
    private String headshot;
    private String sex;
    private String region;
    private String briefIntroduction;


}

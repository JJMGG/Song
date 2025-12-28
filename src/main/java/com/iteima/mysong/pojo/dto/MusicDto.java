package com.iteima.mysong.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;

import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicDto implements Serializable {
    private int userId;
    private int songId;
    private int singerId;
}

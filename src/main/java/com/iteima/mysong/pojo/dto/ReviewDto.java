package com.iteima.mysong.pojo.dto;


import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDto {
    private int page;
    private int size;
    private String search;
    private String status;
    private String sort;

    public String getOrderBy() {
        if (StringUtils.isBlank(status)) {
            return null;
        }
        return status + " " + sort;
    }
}

package com.iteima.mysong.main.service;

import com.iteima.mysong.pojo.Vo.ReviewVo;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    String getMusicFile(MultipartFile file);

    void putSubmit(ReviewVo reviewVo);
}

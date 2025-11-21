package com.iteima.mysong.main.service.impl;


import com.iteima.mysong.main.mapper.UploadMapper;
import com.iteima.mysong.main.service.FileStorageService;
import com.iteima.mysong.main.service.UploadService;
import com.iteima.mysong.pojo.Vo.ReviewVo;
import com.iteima.mysong.pojo.entity.Review;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadServiceImpl implements UploadService {
      @Autowired
     public UploadMapper uploadMapper;

      @Autowired
      public MinioService minioService;


    @Override
    public String getMusicFile(MultipartFile file) {

            String fileUrl =minioService.uploadFile(file);
            return fileUrl;

    }

    @Override
    public void putSubmit(ReviewVo reviewVo) {
        Review review=new Review();
       BeanUtils.copyProperties(reviewVo,review);

        review.setReviewStatus("0");
        uploadMapper.putSubmit(review);
    }
}

package com.iteima.mysong.main.service;

import com.github.pagehelper.PageInfo;
import com.iteima.mysong.common.CommonPageResult;
import com.iteima.mysong.pojo.dto.ReviewDto;
import com.iteima.mysong.pojo.entity.Review;

import java.util.List;

public interface ReviewService {
    CommonPageResult<Review> GetReviewList(ReviewDto reviewDto);

    void approveReview(int reviewId);

    void rejectReview(int reviewId);
}

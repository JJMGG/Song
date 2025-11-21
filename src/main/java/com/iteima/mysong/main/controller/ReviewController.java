package com.iteima.mysong.main.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.iteima.mysong.common.CommonPageResult;
import com.iteima.mysong.common.Result;
import com.iteima.mysong.main.service.impl.ReviewServiceimpl;
import com.iteima.mysong.pojo.dto.ReviewDto;
import com.iteima.mysong.pojo.entity.Review;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
@Slf4j
public class ReviewController {
    @Autowired
    private ReviewServiceimpl reviewServiceimpl;

    @GetMapping("/review/list")
    public Result<CommonPageResult<Review>> GetReviewList(ReviewDto reviewDto)
    {
        System.out.println(reviewDto);
       CommonPageResult<Review> pageResult=reviewServiceimpl.GetReviewList(reviewDto);
        return Result.success(pageResult);
    }

    @PostMapping("/review/approve/{reviewId}")
    public Result approveReview(@PathVariable("reviewId") int reviewId)
    {
        reviewServiceimpl.approveReview(reviewId);
        return Result.success();
    }
    @PostMapping("/review/reject/{reviewId}")
    public Result rejectReview(@PathVariable("reviewId") int reviewId)
    {
        reviewServiceimpl.rejectReview(reviewId);
        return Result.success();
    }
}

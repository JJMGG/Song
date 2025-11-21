package com.iteima.mysong.main.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.iteima.mysong.common.CommonPageResult;
import com.iteima.mysong.common.Tool;
import com.iteima.mysong.main.mapper.ReviewMapper;
import com.iteima.mysong.main.service.ReviewService;
import com.iteima.mysong.pojo.dto.ReviewDto;
import com.iteima.mysong.pojo.entity.Review;
import com.iteima.mysong.pojo.entity.Songs;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
@Slf4j
public class ReviewServiceimpl implements ReviewService {
    @Autowired
    private ReviewMapper reviewMapper;
    @Override
    public CommonPageResult<Review> GetReviewList(ReviewDto reviewDto) {

        // 开启分页
        PageHelper.startPage(reviewDto.getPage(), reviewDto.getSize());

// 查询数据
        List<Review> reviews = reviewMapper.GetReviewList(reviewDto);

// 使用 PageInfo 来获取分页信息
        PageInfo<Review> pageInfo = new PageInfo<>(reviews);

// 创建分页结果对象
        CommonPageResult<Review> result = new CommonPageResult<>();

// 设置分页数据
        result.setList(pageInfo.getList());  // 当前页的评论数据列表
        result.setTotal(pageInfo.getTotal());  // 总记录数
        result.setPageNum(pageInfo.getPageNum());  // 当前页码
        result.setPageSize(pageInfo.getPageSize());  // 每页大小

// 返回分页结果
        return result;
    }

    @Override
    public void approveReview(int reviewId) {
        reviewMapper.approveReview(reviewId,1);
        Review review=reviewMapper.GetReviewOne(reviewId);
        int time=0;
        try {
            time = Tool.getDurationWithFFmpeg(review.getReviewFilepath());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(time);
        Songs song =new Songs();
        song.setSongName(review.getReviewSongName());
        song.setSongAlbum((long) review.getReviewSongAlbum());
        song.setSongFilepath(review.getReviewFilepath());
        song.setSongImg(review.getReviewImg());
        song.setSongLyc(review.getReviewLyc());
        song.setSongNumber(0L);
        song.setSongSinger((long) review.getReviewSinger());
        song.setSongTime(time);
        song.setSongType(review.getReviewType());
        reviewMapper.addsong(song);

    }

    @Override
    public void rejectReview(int reviewId) {
        reviewMapper.rejectReview(reviewId,2);
    }
}

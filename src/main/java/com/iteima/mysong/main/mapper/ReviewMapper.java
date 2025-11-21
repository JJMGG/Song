package com.iteima.mysong.main.mapper;

import com.iteima.mysong.pojo.dto.ReviewDto;
import com.iteima.mysong.pojo.entity.Review;
import com.iteima.mysong.pojo.entity.Songs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Mapper
public interface ReviewMapper {


    List<Review> GetReviewList(ReviewDto reviewDto);


    @Update("update review set review_status=#{i} where review_id=#{reviewId}")
    void approveReview(int reviewId, int i);

    @Update("update review set review_status=#{i} where review_id=#{reviewId}")
    void rejectReview(int reviewId, int i);

    @Select("select * from review where review_id=#{reviewId} ")
    Review GetReviewOne(int reviewId);

    @Insert("insert into songs (song_name,song_singer,song_filepath,song_album,song_lyc,song_img,song_number,song_type,song_time) values (" +
            "#{songName},#{songSinger},#{songFilepath},#{songAlbum},#{songLyc},#{songImg},#{songNumber},#{songType},#{songTime})")
    void addsong(Songs song);
}

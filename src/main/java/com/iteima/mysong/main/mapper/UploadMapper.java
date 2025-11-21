package com.iteima.mysong.main.mapper;

import com.iteima.mysong.pojo.Vo.ReviewVo;
import com.iteima.mysong.pojo.entity.Comments;
import com.iteima.mysong.pojo.entity.Review;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UploadMapper {
    @Insert("insert into review(review_songname,review_singer,review_filepath,review_songalbum,review_lyc,review_img,review_type,review_time,review_status) " +
            "values (#{reviewSongName},#{reviewSinger},#{reviewFilepath},#{reviewSongAlbum},#{reviewLyc},#{reviewImg},#{reviewType},#{reviewTime},#{reviewStatus})")
    void putSubmit(Review review);


}

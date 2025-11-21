package com.iteima.mysong.main.mapper;

import com.iteima.mysong.pojo.Vo.MusicListVo;
import com.iteima.mysong.pojo.Vo.RankSongVo;
import com.iteima.mysong.pojo.Vo.SongListVo;
import com.iteima.mysong.pojo.entity.Songs;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MyMusciMapper {


    @Select("SELECT usersong_songid FROM usersong where usersong_userid=#{userId} ")
    List<Integer> getMusic(Integer userId);

    @Insert("INSERT INTO usersong (usersong_userid,usersong_songid)" +
            " values (#{userId},#{songId})")
    void addMusic(Integer songId, Integer userId);

    @Delete("DELETE from usersong where usersong_userid=#{userId} and usersong_songid=#{songId}")
    void delMusic(Integer userId, Integer songId);


    @Select("select * from songs where song_id in \n" +
            "(\n" +
            "select usersong_songid from usersong where usersong_userid=#{userId}\n" +
            ")\n ")
    List<Songs> getMusics(Integer userId);

    @Select("select list_id,list_title,list_img from lists where list_id in (select  userlist_listid from userlist where userlist_userid=#{userId})\n" +
            "order by list_id asc  ")
    List<MusicListVo> getMusicList(Integer userId);

    @Select("select * from lists where list_type like  concat('%', #{type}, '%') ")
    List<SongListVo> getSort(String type);

    @Update("update songs set song_number=song_number+1 where song_id=#{songId}")
    void addNumber(Integer songId);

    @Select("select * from songs order by song_number desc limit 10")
    List<Songs> getRanking();

    @Select("select * from songs")
    List<Songs> testsongs();

    @Update("update songs set song_time=#{time} where song_id=#{songId}")
    void addTime(Integer time, Long songId);

    @Select("select * from songs where song_name like concat('%', #{name}, '%') ")
    List<Songs> serachsongs(String name);

    @Select("select * from songs where song_singer  in ( select user_id from user where user_name like concat('%', #{name}, '%') ) ")
    List<Songs> serachName(String name);
}

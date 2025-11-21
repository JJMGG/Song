package com.iteima.mysong.main.mapper;

import com.iteima.mysong.pojo.Vo.ListVo;
import com.iteima.mysong.pojo.dto.PersonDto;
import com.iteima.mysong.pojo.entity.Songs;
import com.iteima.mysong.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


import java.util.List;

@Mapper
public interface LoginMapper {
    @Select("select * from user where account=#{account}")
    User login(String account);

    @Select("select * from songs \n" +
            "where song_id in (\n" +
            "select songlist_songid from songlist\n" +
            "where songlist_id =\n" +
            "(\n" +
            "select list_id from  lists\n" +
            "where list_playnum =\n" +
            " (select max(list_playnum) from lists))\n" +
            " )")
    List<Songs> Getsongs();

    @Select("SELECT list_id,list_title,list_img FROM lists")
    List<ListVo> getList();


    void update(PersonDto personDto);

    @Update("update songs set song_filepath=#{s} where song_id=#{id}")
    void changeName(String s,Integer id);
}

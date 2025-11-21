package com.iteima.mysong.main.mapper;

import com.iteima.mysong.pojo.Vo.CommentsVo;
import com.iteima.mysong.pojo.Vo.ListVo;
import com.iteima.mysong.pojo.dto.CollectDto;
import com.iteima.mysong.pojo.entity.*;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SongListMapper {
    @Select("SELECT * FROM lists where list_id=#{id}")
    SongList getSongList(int id);


    @Select("SELECT songlist_songid FROM songlist where songlist_id=#{listId}")
    List<Integer> getSongsId(int listId);

    @Select("SELECT * FROM songs where song_id=#{i}")
    Songs getSong(Integer i);

    @Insert("INSERT INTO userlist (userlist_userid,userlist_listid) values (#{userId},#{listId})")
    void AddCollect(CollectDto collectDto);

    @Delete("DELETE FROM userlist where userlist_userid=#{userId} and userlist_listid=#{listId} ")
    void delcollect(CollectDto collectDto);

    @Select("SELECT * FROM userlist where  userlist_userid=#{userId} and userlist_listid=#{id}")
    UserList isCollect(int id, int userId);

    @Select("SELECT userlist_listid FROM userlist WHERE userlist_userid=#{userId}")
    List<Integer> getListId(int userId);

    @Select("SELECT list_id,list_title,list_img FROM lists WHERE list_id=#{listId}")
    ListVo getcollectList(Integer listId);

    @Insert("insert into comments(comm_id,comm_details,comm_time,comm_userid,comm_type,comm_targetid,comm_father,comm_replyid) " +
            "values (#{commId},#{commDetails},#{commTime},#{commUserid},#{commType},#{commTargetid},#{commFather},#{commReplyid})")
    void savComment(Comments comments);

    @Select("select  * from comments where comm_targetid=#{id}")
    List<CommentsVo> getCommnets(Integer id);

    @Select("select user_name from user where user_id=#{commUserid}")
    String getcommUserName(int commUserid);

    @Select("select headshot from user where user_id=#{commUserid} ")
    String getcommHeadShot(int commUserid);
    @Select("select account from user where user_id=#{commUserid}")
    int getUserAccount(int commUserid);

    @Select("select comm_id from comments where comm_time=#{commTime}")
    int getcommId(LocalDateTime commTime);


    @Select("select comm_details from comments where comm_id=#{messageSendCommentId}")
    String getcontent(int messageSendCommentId);

    @Insert("insert into message(message_id,message_sender_id,message_send_comment_id,message_avatar,message_sender_content,message_time,message_isread,message_type,message_replyer_comment_id,message_replyer_id,message_replyer_content,message_isyou) " +
            "values (#{messageId},#{messageSenderId},#{messageSendCommentId},#{messageAvatar},#{messageSenderContent},#{messageTime},#{messageIsread},#{messageType},#{messageReplyerCommentId},#{messageReplyerId},#{messageReplyerContent},#{messageIsyou})")
    void saveMessage(Message message);

    @Select("select * from message where message_replyer_id=#{id}")
    List<Message> getMessage(String id);

    @Select("select * from message where message_sender_id=#{senderId} and message_replyer_id=#{receiverId} and message_time=#{time}")
    Message findMessage(String senderId, String receiverId, LocalDateTime time);

    @Update("update message set message_isread=#{is_read} where message_id=#{id}")
    void updataRead(Integer id,String is_read);

    @Delete("DELETE FROM message where message_id=#{messageId} ")
    void delMessage(Integer messageId);

    @Select("select * from message where message_replyer_id=#{id}  and message_isread=#{number}")
    List<Message> getflag(Integer id, String number);
}

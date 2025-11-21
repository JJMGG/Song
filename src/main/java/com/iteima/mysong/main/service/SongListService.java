package com.iteima.mysong.main.service;

import com.iteima.mysong.pojo.Vo.CommentsVo;
import com.iteima.mysong.pojo.Vo.ListVo;
import com.iteima.mysong.pojo.Vo.MessageVo;
import com.iteima.mysong.pojo.Vo.SongListVo;
import com.iteima.mysong.pojo.dto.CollectDto;
import com.iteima.mysong.pojo.entity.Comments;
import com.iteima.mysong.pojo.entity.Message;
import com.iteima.mysong.pojo.entity.SongList;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


public interface SongListService {
    SongListVo getSongList(int id,Integer userId);


    void AddCollect(CollectDto collectDto);

    void delcollect(CollectDto collectDto);

    List<ListVo> getcollectList(int userId);

    String saveCommnet(Comments comments);

    List<CommentsVo> getCommnets(Integer id);

    List<MessageVo> getmessage(Integer id);


    Message findMessage(String senderId, String receiverId, LocalDateTime time);

    void updataRead(Integer id);

    void delMessage(Integer messageId);

    String getflag(Integer id);
}

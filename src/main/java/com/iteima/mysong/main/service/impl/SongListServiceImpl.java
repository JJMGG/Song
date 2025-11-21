package com.iteima.mysong.main.service.impl;

import com.alibaba.fastjson.JSON;
import com.iteima.mysong.main.mapper.SongListMapper;
import com.iteima.mysong.main.service.SongListService;
import com.iteima.mysong.pojo.Vo.*;
import com.iteima.mysong.pojo.dto.CollectDto;
import com.iteima.mysong.pojo.entity.*;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;


import java.io.BufferedInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.*;
import java.io.File;
import java.util.stream.Collectors;


@Service
public class SongListServiceImpl implements SongListService {

    @Autowired
    public SongListMapper songListMapper;

    @Override
    public SongListVo getSongList(int id,Integer userId) {
        SongList songList=new SongList();
        songList=songListMapper.getSongList(id);
        System.out.println(songList);
        SongListVo songListVo=new SongListVo();
        BeanUtils.copyProperties(songList,songListVo);
        String[] str=songList.getListType().split(",");
        List<String> strList=new ArrayList<>();
        for (String s : str) {
            strList.add(s);
        }
        List<Integer> list =new ArrayList<>();
        songListVo.setListType(strList);
        list=songListMapper.getSongsId(songList.getListId());
        List<Songs> songs=new ArrayList<>();
        for (Integer i : list) {
            songs.add(songListMapper.getSong(i));
        }
        UserList userList=songListMapper.isCollect(id,userId);
        if(userList!=null)
        {
            songListVo.setIscollect(true);
        }
        else
            songListVo.setIscollect(false);
        songListVo.setSongs(songs);
        return songListVo;
    }

    @Override
    public void AddCollect(CollectDto collectDto) {
        songListMapper.AddCollect(collectDto);

    }

    @Override
    public void delcollect(CollectDto collectDto) {
        songListMapper.delcollect(collectDto);
    }

    @Override
    public List<ListVo> getcollectList(int userId) {
        List<Integer> listNumber=songListMapper.getListId(userId);
        List<ListVo> list=new ArrayList<>();
        for (Integer listid : listNumber) {
            list.add(songListMapper.getcollectList(listid));
        }
        System.out.println(list);
        return list;
    }

    @Override
    public String saveCommnet(Comments comments) {


        LocalDateTime now = LocalDateTime.now();
        LocalDateTime truncatedTime = now.truncatedTo(ChronoUnit.SECONDS);
        comments.setCommTime(truncatedTime);
        comments.setCommId(0);
        songListMapper.savComment(comments);
        int tempCommId=songListMapper.getcommId(truncatedTime);//!!!!消息出现bug

        if(comments.getCommFather()!=0)
        {
            System.out.println(tempCommId);
            Message message=new Message();
            message.setMessageSenderId(comments.getCommUserid()+"");
            message.setMessageSendCommentId(tempCommId);
            message.setMessageAvatar(songListMapper.getcommHeadShot(comments.getCommUserid()));
            message.setMessageSenderContent(songListMapper.getcontent(tempCommId));
            message.setMessageTime(truncatedTime);
            message.setMessageIsread("0");
            message.setMessageType("reply");
            message.setMessageReplyerCommentId(songListMapper.getcommId(comments.getCommTime()));
            message.setMessageReplyerId(comments.getCommReplyid()+"");
            message.setMessageReplyerContent(songListMapper.getcontent(message.getMessageReplyerCommentId()));
            message.setMessageIsyou("1");

            songListMapper.saveMessage(message);

        }
        return truncatedTime.toString();
    }

    @Override
    public List<CommentsVo> getCommnets(Integer id) {
         List<CommentsVo> commnet = songListMapper.getCommnets(id);
          List<CommentsVo> root=commnet.stream().filter(commnetTemp->commnetTemp.getCommFather()==0).collect(Collectors.toList());
        for (CommentsVo commentRoot : root) {
            commentRoot.setChildren(commnet.stream().filter(commnetTemp->commnetTemp.getCommFather()==commentRoot.getCommId()).collect(Collectors.toList()));
            for (CommentsVo child : commentRoot.getChildren()) {
                child.setCommUserName(songListMapper.getcommUserName(child.getCommUserid()));
                child.setCommHeadShot(songListMapper.getcommHeadShot(child.getCommUserid()));
                child.setCommReplyName(songListMapper.getcommUserName(child.getCommReplyid()));
            }
            commentRoot.setCommUserName(songListMapper.getcommUserName(commentRoot.getCommUserid()));
            commentRoot.setCommHeadShot(songListMapper.getcommHeadShot(commentRoot.getCommUserid()));
        }
         return root;

    }


    @Override
    public List<MessageVo> getmessage(Integer id) {
        System.out.println("进入Message数据层");
        List<MessageVo> messageList=new ArrayList<>();
        List<Message> message=songListMapper.getMessage(id+"");
        for (Message messageTemp : message) {

            MessageVo messageVo=new MessageVo();
            messageTemp.setMessageSenderId(messageTemp.getMessageSenderId()+"-"+songListMapper.getcommUserName(Integer.parseInt(messageTemp.getMessageSenderId())));
            messageTemp.setMessageReplyerId(messageTemp.getMessageReplyerId()+"-"+songListMapper.getcommUserName(Integer.parseInt(messageTemp.getMessageReplyerId())));
            BeanUtils.copyProperties(messageTemp, messageVo);
            ReplyToVo replyToVo=new ReplyToVo();
            replyToVo.setMessageIsyou(true);
            replyToVo.setMessageReplyerContent(messageTemp.getMessageReplyerContent());
            replyToVo.setMessageReplyerId(messageTemp.getMessageReplyerId());
            replyToVo.setMessageReplyerCommentId(messageTemp.getMessageReplyerCommentId());
            messageVo.setReplyTo(replyToVo);
            messageVo.setMessageSenderContent(messageTemp.getMessageReplyerContent());
            replyToVo.setMessageReplyerContent(messageTemp.getMessageSenderContent());
            String isRead=messageTemp.getMessageIsread();
            System.out.println(isRead+"是否已读");
            if(isRead.equals("0"))
                messageVo.setMessageIsread(false);
            else
                messageVo.setMessageIsread(true);
            messageList.add(messageVo);
        }
       System.out.println(messageList.toString());
       return messageList;
    }


    /**
     * 根据发送者和接收者id和发送时间查找消息，将消息加入一个消息列表
     * @param senderId
     * @param receiverId
     * @param time
     * @return
     */

    @Override
    public Message findMessage(String senderId, String receiverId, LocalDateTime time) {
       return songListMapper.findMessage(senderId,receiverId,time);
    }

    @Override
    public void updataRead(Integer id) {
        songListMapper.updataRead(id,"1");
        System.out.println("修改成功");
    }

    @Override
    public void delMessage(@RequestBody Integer messageId) {
        System.out.println(messageId+"这是删除的id");
        songListMapper.delMessage(messageId);
    }

    @Override
    public String getflag(Integer id) {
        List<Message> messages=songListMapper.getflag(id,"0");

        if(messages.size()!=0)
            return "true";//表示有未读消息
        return "false";//表示没有未读消息
    }

    //将评论回复转为JSon字符发送给前端,这里的sendContent和replyContent交换了一下,其他为基础设置
    public  String tranToMessageVo(Message data)
    {
        MessageVo messageVo=new MessageVo();
        data.setMessageSenderId(data.getMessageSenderId()+"-"+songListMapper.getcommUserName(Integer.parseInt(data.getMessageSenderId())));
        data.setMessageReplyerId(data.getMessageReplyerId()+"-"+songListMapper.getcommUserName(Integer.parseInt(data.getMessageReplyerId())));
        BeanUtils.copyProperties(data, messageVo);
        ReplyToVo replyToVo=new ReplyToVo();
        replyToVo.setMessageIsyou(true);
        replyToVo.setMessageReplyerContent(data.getMessageReplyerContent());
        replyToVo.setMessageReplyerId(data.getMessageReplyerId());
        replyToVo.setMessageReplyerCommentId(data.getMessageReplyerCommentId());
        messageVo.setReplyTo(replyToVo);
        String sendString=messageVo.getMessageSenderContent();
        messageVo.setMessageSenderContent(replyToVo.getMessageReplyerContent());
        replyToVo.setMessageReplyerContent(sendString);
        String Json= JSON.toJSONString(messageVo);
        System.out.println(Json);
        return Json;
    }


}


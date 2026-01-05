package com.iteima.mysong.main.service.impl;

import com.iteima.mysong.main.mapper.LoginMapper;
import com.iteima.mysong.main.mapper.MyMusciMapper;
import com.iteima.mysong.main.service.MyMusicService;
import com.iteima.mysong.pojo.Vo.MusicListVo;
import com.iteima.mysong.pojo.Vo.RankSongVo;
import com.iteima.mysong.pojo.Vo.SongListVo;
import com.iteima.mysong.pojo.Vo.SongVo;
import com.iteima.mysong.pojo.entity.Songs;
import com.iteima.mysong.pojo.entity.UserMusicInteractions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class MyMusicServiceImpl implements MyMusicService {

    @Autowired
    private MyMusciMapper myMusciMapper;

    @Autowired
    private LoginMapper loginMapper;

    @Override
    public List<Integer> getMusic(Integer userId) {
       return myMusciMapper.getMusic(userId);
    }

    @Override
    public void addMusic(Integer songId, Integer userId,Integer singerId) {
        myMusciMapper.addMusic(songId,userId);

//        添加一条用户交互行为
        UserMusicInteractions userMusicInteractions=new UserMusicInteractions();
        userMusicInteractions.setUserId(userId);
        userMusicInteractions.setMusicId(songId);
        userMusicInteractions.setActionType("like");
        userMusicInteractions.setIsLiked(1);
        userMusicInteractions.setInteractionTime(LocalDateTime.now());
        userMusicInteractions.setSingerId(singerId);
        myMusciMapper.addUserInteraction(userMusicInteractions);
    }

    @Override
    public void delMusic(Integer userId, Integer songId,Integer singerId) {
        myMusciMapper.delMusic(userId,songId);
        //        添加一条用户交互行为
        UserMusicInteractions userMusicInteractions=new UserMusicInteractions();
        userMusicInteractions.setUserId(userId);
        userMusicInteractions.setMusicId(songId);
        userMusicInteractions.setActionType("delete");

        userMusicInteractions.setInteractionTime(LocalDateTime.now());
        userMusicInteractions.setSingerId(singerId);
        myMusciMapper.addUserInteraction(userMusicInteractions);
    }

    @Override
    public List<SongVo> getMusics(Integer id, Integer userId) {
          List<Songs> list=myMusciMapper.getMusics(id);
        List<SongVo> ans=new ArrayList<>();

        for (Songs songs : list) {
            SongVo temp=new SongVo();
            BeanUtils.copyProperties(songs,temp);
            temp.setSongSinger(loginMapper.getSingerName(songs.getSongSinger())+"-"+songs.getSongSinger());
            ans.add(temp);
        }
        return ans;
    }

    @Override
    public List<MusicListVo>  getMusicList(Integer userId) {
        List<MusicListVo> list=myMusciMapper.getMusicList(userId);
        log.info("list:{}",list);
        return list;
    }

    @Override
    public List<SongListVo> getSort(String type) {
        List<SongListVo> list=myMusciMapper.getSort(type);
        return list;
    }

    @Override
    public void addNumber(Integer songId,Integer userId,Integer playTime) {
        myMusciMapper.addNumber(songId);
        List<Integer> list= myMusciMapper.bySongIdGetListId(songId);

        //这里对歌单点击次数进行一次自增,通过听歌时长来进行调用

        for (Integer i : list) {
            myMusciMapper.addListNumber(i);
        }


        //        添加一条用户交互行为
        UserMusicInteractions userMusicInteractions=new UserMusicInteractions();
        userMusicInteractions.setUserId(userId);
        userMusicInteractions.setMusicId(songId);
        userMusicInteractions.setActionType("play");

        userMusicInteractions.setInteractionTime(LocalDateTime.now());

        userMusicInteractions.setPlayDuration(playTime);
        myMusciMapper.addUserInteraction(userMusicInteractions);
    }

    @Override
    public List<SongVo> getRanking() {
        List<Songs> list=myMusciMapper.getRanking();
        List<SongVo> ans =new ArrayList<>();
        for (Songs songs : list) {
            SongVo temp=new SongVo();
            BeanUtils.copyProperties(songs,temp);
            temp.setSongSinger(loginMapper.getSingerName(songs.getSongSinger())+"-"+songs.getSongSinger());
            ans.add(temp);
        }
        return ans;
    }

    @Override
    public void testsongs() {
        List<Songs> list=myMusciMapper.testsongs();
        for (Songs songs : list) {
            try {
                Integer time=getDurationWithFFmpeg(songs.getSongFilepath());
                myMusciMapper.addTime(time,songs.getSongId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<Songs> serachsongs(String name,Integer userId) {
        List<Songs> list=new ArrayList<>();
        list=myMusciMapper.serachsongs(name);
        List<Songs> Templist=serachName(name);
        list.addAll(Templist);
        //        添加一条用户交互行为
        UserMusicInteractions userMusicInteractions=new UserMusicInteractions();
        userMusicInteractions.setUserId(userId);
        userMusicInteractions.setActionType("search");
        userMusicInteractions.setSearchContent(name);
        userMusicInteractions.setInteractionTime(LocalDateTime.now());
        myMusciMapper.addUserInteraction(userMusicInteractions);
        return list;
    }


    public List<Songs> serachName(String name) {
        List<Songs> list=new ArrayList<>();
        list=myMusciMapper.serachName(name);
        return list;
    }


    public int getDurationWithFFmpeg(String audioUrl) throws IOException {
        Process process = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                audioUrl
        ).start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine().trim(); // 清理换行符和空格
            double durationSeconds = Double.parseDouble(output);
            return (int)durationSeconds;   // 直接传递秒数
        }
    }
    private String formatDuration(double totalSeconds) {
        int totalMinutes = (int) (totalSeconds / 60);
        int remainingSeconds = (int) (totalSeconds % 60);
        return String.format("%d:%02d", totalMinutes, remainingSeconds); // 如 "4:31"
    }


}

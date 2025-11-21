package com.iteima.mysong.main.service.impl;

import com.iteima.mysong.main.mapper.MyMusciMapper;
import com.iteima.mysong.main.service.MyMusicService;
import com.iteima.mysong.pojo.Vo.MusicListVo;
import com.iteima.mysong.pojo.Vo.RankSongVo;
import com.iteima.mysong.pojo.Vo.SongListVo;
import com.iteima.mysong.pojo.entity.Songs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class MyMusicServiceImpl implements MyMusicService {

    @Autowired
    private MyMusciMapper myMusciMapper;



    @Override
    public List<Integer> getMusic(Integer userId) {
       return myMusciMapper.getMusic(userId);
    }

    @Override
    public void addMusic(Integer songId, Integer userId) {
        myMusciMapper.addMusic(songId,userId);
    }

    @Override
    public void delMusic(Integer userId, Integer songId) {
        myMusciMapper.delMusic(userId,songId);
    }

    @Override
    public List<Songs> getMusics(Integer id, Integer userId) {
          List<Songs> list=myMusciMapper.getMusics(id);
        return list;
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
    public void addNumber(Integer songId) {
        myMusciMapper.addNumber(songId);
    }

    @Override
    public List<Songs> getRanking() {
        List<Songs> list=myMusciMapper.getRanking();
        return list;
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
    public List<Songs> serachsongs(String name) {
        List<Songs> list=new ArrayList<>();
        list=myMusciMapper.serachsongs(name);
        List<Songs> Templist=serachName(name);
        list.addAll(Templist);

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

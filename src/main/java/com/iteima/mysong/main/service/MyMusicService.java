package com.iteima.mysong.main.service;

import com.iteima.mysong.pojo.Vo.MusicListVo;
import com.iteima.mysong.pojo.Vo.RankSongVo;
import com.iteima.mysong.pojo.Vo.SongListVo;
import com.iteima.mysong.pojo.Vo.SongVo;
import com.iteima.mysong.pojo.entity.Songs;

import java.util.List;

public interface MyMusicService {
    List<Integer> getMusic(Integer userId);

    void addMusic(Integer songId, Integer userId,Integer singerId);

    void delMusic(Integer userId, Integer songId,Integer singerId);

    List<SongVo> getMusics(Integer id, Integer userId);

     List<MusicListVo> getMusicList(Integer userId);

    List<SongListVo> getSort(String type);

    void addNumber(Integer songId,Integer userId,Integer playTime);

    List<SongVo> getRanking();

    void testsongs();

    List<Songs> serachsongs(String name,Integer userId);


}

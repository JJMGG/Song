package com.iteima.mysong.main.controller;

import com.iteima.mysong.main.service.impl.MyMusicServiceImpl;
import com.iteima.mysong.common.Result;
import com.iteima.mysong.pojo.Vo.MusicListVo;
import com.iteima.mysong.pojo.Vo.RankSongVo;
import com.iteima.mysong.pojo.Vo.SongListVo;
import com.iteima.mysong.pojo.dto.MusicDto;
import com.iteima.mysong.pojo.entity.Songs;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
@Slf4j
public class MyMusiController {
    @Autowired
    private MyMusicServiceImpl myMusicService;

    @GetMapping("/music")
    /**
     * 返回收藏了哪些歌曲
     */
    public Result<List<Integer>> getMusic(Integer userId)
    {
        List<Integer> list= myMusicService.getMusic(userId);
        return Result.success(list);
    }


    @PutMapping("/addmusic")
    public Result addMusic(@RequestBody MusicDto musicDto)
    {

        log.info("songid:{},userid:{},singerid:{}",musicDto.getSongId(),musicDto.getUserId(),musicDto.getSingerId());
        myMusicService.addMusic(musicDto.getSongId(),musicDto.getUserId(),musicDto.getSingerId());

        return Result.success();
    }


    @DeleteMapping("/delmusic")
    public Result delMusic(@RequestBody MusicDto musicDto)
    {
        myMusicService.delMusic(musicDto.getUserId(),musicDto.getSongId(),musicDto.getSingerId());
        return Result.success();
    }

   @GetMapping("/musics")
    public  Result<List<Songs>> getMusics(@RequestParam Integer id,@RequestParam Integer userId)
   {
       log.info("id:{},userId:{}",id,userId);
       List<Songs> list=myMusicService.getMusics(id,userId);
       return Result.success(list);
   }

   @GetMapping("/musiclist")
    public Result<List<MusicListVo>> getMusicList( Integer userId)
   {
       System.out.println(userId);
       List<MusicListVo> musicListVo=myMusicService.getMusicList(userId);
       return Result.success(musicListVo);
   }
   @GetMapping("/sort")
    public Result<List<SongListVo>> getSort(String type)
   {
       log.info("{}",type);
       List<SongListVo> list=myMusicService.getSort(type);
       return Result.success(list);
   }

//   自增音乐次数
   @PutMapping("/addNumber")
    public Result addNumber(@RequestBody Map<String, Integer> request)
   {    Integer songId = request.get("songId");
        Integer userId = request.get("userId");
        Integer singerId = request.get("singerId");
        Integer playTime = request.get("playTime");

       myMusicService.addNumber(songId,userId,playTime);

       return Result.success();
   }


   @GetMapping("/ranking")
    public Result<List<Songs>> getRanking()
   {
       List<Songs> list=myMusicService.getRanking();
       return Result.success(list);
   }

   @GetMapping("/search")
   public Result<List<Songs>> serach(String name,Integer userId)
   {
       System.out.println(name+"这是搜索关键词");
       List<Songs> list=myMusicService.serachsongs(name,userId);
       System.out.println(list);
       return Result.success(list);
   }

   @GetMapping("/testsongs")
    public Result testSongs()
   {
       myMusicService.testsongs();
       return Result.success();
   }

}

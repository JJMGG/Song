package com.iteima.mysong.main.controller;

import com.iteima.mysong.common.Result;
import com.iteima.mysong.main.service.impl.SongListServiceImpl;
import com.iteima.mysong.pojo.Vo.CommentsVo;
import com.iteima.mysong.pojo.Vo.ListVo;
import com.iteima.mysong.pojo.Vo.MessageVo;
import com.iteima.mysong.pojo.Vo.SongListVo;
import com.iteima.mysong.pojo.dto.CollectDto;
import com.iteima.mysong.pojo.entity.Comments;
import com.iteima.mysong.pojo.entity.SongList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@RestController
@RequestMapping("/api")
@CrossOrigin
@Slf4j
public class SongListController {

     @Autowired
     private SongListServiceImpl songListService;



    @GetMapping("/songList")
    public Result<SongListVo> getSongList(@RequestParam  int id,@RequestParam Integer userId)
    {

        SongListVo songListVo;
        songListVo=songListService.getSongList(id,userId);
        System.out.println(userId);
        return Result.success(songListVo);
    }
    @PostMapping("/collect")
    public Result collect(@RequestBody CollectDto collectDto)
    {
        System.out.println(collectDto);
        songListService.AddCollect(collectDto);
        return Result.success();
    }

    @DeleteMapping("/delcollect")
    public Result delcollect(@RequestBody CollectDto collectDto)
    {
        songListService.delcollect(collectDto);
        return Result.success();
    }
   @GetMapping("/collectList/{userId}")
    public Result<List<ListVo>> getCollectList(@PathVariable int userId)
   {
       List<ListVo> list=songListService.getcollectList(userId);
       return Result.success(list);
   }

   @PostMapping("/comment")
    public Result saveCommnet(@RequestBody Comments comments)
   {
       String dataTime = songListService.saveCommnet(comments);
       return  Result.success(dataTime);
   }
   @GetMapping("/comments")
    public Result<List<CommentsVo>> getCommnets(Integer id)
   {
       List<CommentsVo> commentList=songListService.getCommnets(id);
       return Result.success(commentList);
   }

   @GetMapping("/message")
   public Result<List<MessageVo>> getmessage(Integer id )
   {
       System.out.println("这是id:"+id);
       List<MessageVo> messageList=songListService.getmessage(id);
       return Result.success(messageList);
   }

   @PutMapping("/updataRead")
    public Result updataRead(@RequestBody Map<String, Integer> request)
   {Integer messageId = request.get("messageId");

       songListService.updataRead(messageId);
       return Result.success();
   }

    @DeleteMapping("/delMessage")
    public Result delcollect(@RequestBody Map<String, Integer> request)
    {Integer messageId = request.get("messageId");
        songListService.delMessage(messageId);
        return Result.success();
    }

    @GetMapping("/flag")
    public Result<String> tranToMessageVo(Integer id)
    {
        String message=songListService.getflag(id);
        return Result.success(message);
    }
}

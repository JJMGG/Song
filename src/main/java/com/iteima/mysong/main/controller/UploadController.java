package com.iteima.mysong.main.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iteima.mysong.common.Result;
import com.iteima.mysong.main.service.impl.UploadServiceImpl;
import com.iteima.mysong.pojo.Vo.ReviewVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/api")
@RestController
@CrossOrigin
public class UploadController {
    @Autowired
    private UploadServiceImpl uploadServiceimpl;


    @PostMapping("/upload/music")
    public Result<String> getMusicFile(MultipartFile file)
    {
        String musicFileURL = uploadServiceimpl.getMusicFile(file);
        System.out.println("这是视频的数据");
        System.out.println(musicFileURL);
        return Result.success(musicFileURL);
    }
    @PostMapping("/upload/image")
    public Result<String> getImageFile(MultipartFile file)
    {
        String musicFileURL = uploadServiceimpl.getMusicFile((file));
        System.out.println("这是图片的数据");
        System.out.println(musicFileURL);
        return Result.success(musicFileURL);
    }
    @GetMapping("/upload/singer")
    public Result<JSONArray> getSinger()
    {
        JSONArray singersArray = new JSONArray();

        // 创建每个歌手的 JSON 对象并添加到数组
        JSONObject singer1 = new JSONObject();
        singer1.put("id", 1);
        singer1.put("name", "周杰伦");
        JSONObject singer2 = new JSONObject();
        singer2.put("id", 2);
        singer2.put("name", "邓紫棋");
        JSONObject singer3 = new JSONObject();
        singer3.put("id", 3);
        singer3.put("name", "王菲");
        JSONObject singer4 = new JSONObject();
        singer4.put("id", 4);
        singer4.put("name", "李宇春");
        JSONObject singer5 = new JSONObject();
        singer5.put("id", 5);
        singer5.put("name", "陈奕迅");
        // 将歌手对象添加到数组
        singersArray.add(singer1);
        singersArray.add(singer2);
        singersArray.add(singer3);
        singersArray.add(singer4);
        singersArray.add(singer5);
        return Result.success(singersArray);
    }


    @GetMapping("/upload/albums")
    public Result<JSONArray> getAlbums()
    {
        JSONArray singersArray = new JSONArray();

        // 创建每个歌手的 JSON 对象并添加到数组
        JSONObject singer1 = new JSONObject();
        singer1.put("id", 1);
        singer1.put("name", "范特西");
        JSONObject singer2 = new JSONObject();
        singer2.put("id", 2);
        singer2.put("name", "七里香");


        JSONObject singer3 = new JSONObject();
        singer3.put("id", 3);
        singer3.put("name", "泡沫");


        JSONObject singer4 = new JSONObject();
        singer4.put("id", 4);
        singer4.put("name", "我的秘密");


        JSONObject singer5 = new JSONObject();
        singer5.put("id", 5);
        singer5.put("name", "陈奕迅");


        // 将歌手对象添加到数组
        singersArray.add(singer1);
        singersArray.add(singer2);
        singersArray.add(singer3);
        singersArray.add(singer4);
        singersArray.add(singer5);


        return Result.success(singersArray);
    }
    @PostMapping("/upload/submit")
    public Result<String> putSubmit(@RequestBody ReviewVo reviewVo)
    {

        uploadServiceimpl.putSubmit(reviewVo);
        return Result.success("上传成功");
    }
}

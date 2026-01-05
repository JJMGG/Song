package com.iteima.mysong.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.iteima.mysong.common.Jwt;
import com.iteima.mysong.common.JwtUtils;
import com.iteima.mysong.common.Result;
import com.iteima.mysong.main.service.impl.LoginServiceimpl;
import com.iteima.mysong.main.service.impl.MinioService;
import com.iteima.mysong.pojo.Vo.ListVo;
import com.iteima.mysong.pojo.Vo.SongVo;
import com.iteima.mysong.pojo.Vo.UserVo;
import com.iteima.mysong.pojo.dto.PersonDto;
import com.iteima.mysong.pojo.dto.Userdto;
import com.iteima.mysong.pojo.entity.Songs;
import com.iteima.mysong.pojo.entity.User;
import io.minio.errors.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RequestMapping("/api")
@RestController
@CrossOrigin
public class LogInController {

    @Autowired
    public MinioService minioService;

    @Autowired
     public LoginServiceimpl loginServiceimpl;

    @Autowired
    private DefaultKaptcha defaultKaptcha;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/login")
    public Result<UserVo> login(@RequestBody Userdto userdto)
    {
        System.out.println(userdto);
        User user=loginServiceimpl.login(userdto);
        if(user==null)
        {
            return Result.error("登录失败");
        }
        else
        {
            String code=redisTemplate.opsForValue().get("cache:code:"+userdto.getSessionId());
            if(code==null)
                return Result.error("验证码过期");
            if(!code.equals(userdto.getCode()))
                return Result.error("验证码错误");

            Map<String,Object> map=new HashMap<>();
            map.put(Jwt.User_Id,user.getUserId());
            map.put(Jwt.Account,user.getAccount());
            String jwt= JwtUtils.createJwt(map);
            UserVo userVo=new UserVo();
            BeanUtils.copyProperties(user,userVo);
            userVo.setToken(jwt);
            userVo.setPassword("******");
            return Result.success(userVo);
        }
    }

    @GetMapping("/home")
    public Result<List<SongVo>> Getsongs()
    {

        List<SongVo> list=new ArrayList<>();
        list=loginServiceimpl.Getsongs();
        //System.out.println(list);
        return Result.success(list);
    }

    @GetMapping("/List")
    public Result<List<ListVo>> getList()
    {
        List<ListVo> list=loginServiceimpl.getList();
        return Result.success(list);
    }


    /*
    * 修改个人信息
    * */
    @PutMapping("/person")
    public Result updataPerson(@RequestBody PersonDto personDto)
    {
        System.out.println(personDto);
        loginServiceimpl.updataPerson(personDto);
        return Result.success();
    }


    @GetMapping("/Imgcode")
    public void getCaptchaImage(@RequestParam("sessionId") String sessionId, HttpServletResponse response) throws IOException {


        // 生成验证码文本
        String text = defaultKaptcha.createText();

        // 生成验证码图片
        BufferedImage image = defaultKaptcha.createImage(text.split("@")[0]);
        loginServiceimpl.setcode(text,sessionId);

        // 设置响应头
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-store, no-cache");

        // 将图片写入响应流
        try (OutputStream out = response.getOutputStream()) {
            ImageIO.write(image, "jpeg", out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /*添加搜索历史*/
    @PostMapping("/history")
    public Result addHistory(@RequestBody Map<String, Object> requestBody)
    {
        String name = (String) requestBody.get("name");
        Integer userId = Math.toIntExact(((Number) requestBody.get("userId")).longValue());
        System.out.println(name+"----------"+userId);
        loginServiceimpl.addHistory(name,userId);
        return Result.success();
    }

    @GetMapping("/getHistory")
    public Result<List<JSONObject>> getHistory(@RequestParam String userId)
    {
        System.out.println(userId+"这是获取id的历史搜索");
        List<JSONObject> list=loginServiceimpl.getHistory(userId);
        System.out.println(list);
        return Result.success(list);
    }


    @GetMapping("/testsong")
    public Result<Songs> testSong() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {

//        String directoryPath = "E:\\Minio\\音频";
//
//        // 创建File对象
//        File directory = new File(directoryPath);
//
//        // 检查目录是否存在
//        if (directory.exists() && directory.isDirectory()) {
//            // 获取目录中的所有文件和文件夹
//            File[] files = directory.listFiles();
//
//            // 如果目录下有文件
//            if (files != null) {
//                for (File file : files) {
//
//                    // 打印文件或文件夹的名称
//                    System.out.println(file.getName().split("\\.").length);
//        minioService.uploadLocalFile("E:\\Minio\\音频\\"+file.getName(),file.getName());
//        loginServiceimpl.changeName("http://169.254.1.116:9000/mysong/"+file.getName(), Integer.valueOf(file.getName().split("\\.")[0]));
//                }
//            } else {
//                System.out.println("该目录下没有文件");
//            }
//        } else {
//            System.out.println("目录不存在或不是有效的目录");
//        }
        double durationWithFFmpeg = loginServiceimpl.getDurationWithFFmpeg("http://172.16.169.64:9000/mysong/first.mp3");
        System.out.println((int) durationWithFFmpeg);
        return Result.success();
    }


}

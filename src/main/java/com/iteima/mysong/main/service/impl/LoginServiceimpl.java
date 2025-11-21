package com.iteima.mysong.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.iteima.mysong.main.mapper.LoginMapper;
import com.iteima.mysong.main.service.LoginService;
import com.iteima.mysong.pojo.Vo.ListVo;
import com.iteima.mysong.pojo.dto.PersonDto;
import com.iteima.mysong.pojo.dto.Userdto;
import com.iteima.mysong.pojo.entity.Songs;
import com.iteima.mysong.pojo.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static okhttp3.internal.concurrent.TaskLoggerKt.formatDuration;

@Service
@Slf4j
public class LoginServiceimpl implements LoginService {
    @Autowired
    public LoginMapper loginMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SEARCH_HISTORY_KEY = "search:history:";  // Key 前缀
    private static final long EXPIRATION_SECONDS = 1 * 24 * 60 * 60;     // 7 天过期

    public LoginServiceimpl() {
    }

    public LoginServiceimpl(LoginMapper loginMapper, StringRedisTemplate redisTemplate, String SEARCH_HISTORY_KEY, long EXPIRATION_SECONDS, Logger log) {
        this.loginMapper = loginMapper;
//        this.redisTemplate = redisTemplate;
//        this.SEARCH_HISTORY_KEY = SEARCH_HISTORY_KEY;
//        this.EXPIRATION_SECONDS = EXPIRATION_SECONDS;
//        this.log = log;
    }

    @Override
    public User login(Userdto user) {
        User userResult=loginMapper.login(user.getAccount());
        if(userResult!=null) {
            if (user.getPassword().equals(userResult.getPassword())) {
                log.info("这是数据库返回:{}",userResult);
                return userResult;
            }
        }
        return null;
    }

    @Override
    public List<Songs> Getsongs() {
     return loginMapper.Getsongs();
    }

    @Override
    public List<ListVo> getList() {
       List<ListVo>  list=loginMapper.getList();
        return list;
    }

    @Override
    public void updataPerson(PersonDto personDto) {
        loginMapper.update(personDto);
    }

    @Override
    public void setcode(String code,String sessionId) {
        String rusult=code.split("@")[1];
        redisTemplate.opsForValue().set("cache:code:"+sessionId, rusult, 1, TimeUnit.MINUTES);
        return ;
    }

    @Override
    public void addHistory(String name,Integer userId) {
        String key = SEARCH_HISTORY_KEY + userId;
        JSONObject record = new JSONObject();
        record.put("value", name);
        redisTemplate.opsForList().leftPush(key, record.toJSONString());
        redisTemplate.expire(key, EXPIRATION_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public List<JSONObject> getHistory(String userId) {
        String key = SEARCH_HISTORY_KEY + userId;
        List<String> records = redisTemplate.opsForList().range(key, 0, -1);

        return records.stream()
                .map(JSONObject::parseObject)  // 将 String 转为 JSONObject
                .collect(Collectors.toList());
    }

    public void changeName(String s,Integer id) {
        loginMapper.changeName(s,id);
    }

    public double getDurationWithFFmpeg(String audioUrl) throws IOException {
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
            return durationSeconds;   // 直接传递秒数
        }
    }

    /**
     * 获取
     * @return loginMapper
     */
    public LoginMapper getLoginMapper() {
        return loginMapper;
    }

    /**
     * 设置
     * @param loginMapper
     */
    public void setLoginMapper(LoginMapper loginMapper) {
        this.loginMapper = loginMapper;
    }

    /**
     * 获取
     * @return redisTemplate
     */
    public StringRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    /**
     * 设置
     * @param redisTemplate
     */
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String toString() {
        return "LoginServiceimpl{loginMapper = " + loginMapper + ", redisTemplate = " + redisTemplate + ", SEARCH_HISTORY_KEY = " + SEARCH_HISTORY_KEY + ", EXPIRATION_SECONDS = " + EXPIRATION_SECONDS + ", log = " + log + "}";
    }
}

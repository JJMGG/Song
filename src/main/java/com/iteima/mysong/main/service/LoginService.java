package com.iteima.mysong.main.service;

import com.alibaba.fastjson.JSONObject;
import com.iteima.mysong.pojo.Vo.ListVo;
import com.iteima.mysong.pojo.Vo.SongVo;
import com.iteima.mysong.pojo.dto.PersonDto;
import com.iteima.mysong.pojo.dto.Userdto;
import com.iteima.mysong.pojo.entity.Songs;
import com.iteima.mysong.pojo.entity.User;

import java.util.List;

public interface LoginService {
    User login(Userdto user);

    List<SongVo> Getsongs();

    List<ListVo> getList();

    void updataPerson(PersonDto personDto);

    void setcode(String code,String sessionId);

    void addHistory(String name,Integer userId);

    List<JSONObject> getHistory(String userId);
}

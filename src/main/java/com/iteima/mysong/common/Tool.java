package com.iteima.mysong.common;

import com.iteima.mysong.pojo.Vo.MessageVo;
import com.iteima.mysong.pojo.Vo.ReplyToVo;
import org.springframework.beans.BeanUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Tool {
    public static int getDurationWithFFmpeg(String audioUrl) throws IOException {
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

}

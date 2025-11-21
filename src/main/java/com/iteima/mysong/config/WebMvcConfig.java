package com.iteima.mysong.config;

import com.iteima.mysong.common.TokenInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @className: WebMvcConfig
 * @description:webMvc配置
 * @author: sh.Liu
 * @date: 2022-01-13 09:51
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册我们刚刚自定义的TokenInterceptor拦截器
        registry.addInterceptor(new TokenInterceptor())
                // 对所有请求进行拦截
                .addPathPatterns("/api/comment");
                // 排除 login请求

    }
}



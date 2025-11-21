package com.iteima.mysong.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 核心业务逻辑，判断是否登录等
        String token = request.getHeader("token");

//        System.out.println(token);
        if(token==null)
            return false;
        // 正常token是的登录后签发的，前端携带过来
        try {
            JwtUtils.parseJwt(token);
        }
        catch (Exception e)
        {
            System.out.println("请求被拦截");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        //
        //System.out.println("controller 执行完了");
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        //System.out.println("我获取到了一个返回的结果："+response);
        //System.out.println("请求结束了");
    }
}



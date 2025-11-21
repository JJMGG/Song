package com.iteima.mysong.config;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetTokenSeesionConfig extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig config,
                                HandshakeRequest request,
                                HandshakeResponse response) {



        // 获取请求的完整URI（包含Query参数）
        String query = request.getRequestURI().getQuery();

        // 解析Query参数（例如：userId=123）
        if (query != null) {
            Map<String, String> queryParams = parseQuery(query);
            String userId = queryParams.get("userId");

            // 将userId存入WebSocket会话属性
            if (userId != null) {

                config.getUserProperties().put("userId",userId);

            }
        }
        response.getHeaders().put("Access-Control-Allow-Origin", Collections.singletonList("*"));
        response.getHeaders().put("Access-Control-Allow-Headers", Collections.singletonList("*"));

    }
    // 解析Query字符串的工具方法
    private Map<String, String> parseQuery(String query) {
        return Arrays.stream(query.split("&"))
                .map(param -> param.split("="))
                .collect(Collectors.toMap(
                        arr -> arr[0],   // Key (如userId)
                        arr -> arr.length > 1 ? arr[1] : ""  // Value (如123)
                ));
    }
}

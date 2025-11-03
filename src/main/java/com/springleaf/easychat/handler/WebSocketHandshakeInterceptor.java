package com.springleaf.easychat.handler;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 用于验证用户身份并将用户ID存入 WebSocket Session
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 握手前拦截
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            // 从请求参数中获取 token
            String query = request.getURI().getQuery();
            if (query != null && query.contains("token=")) {
                String token = query.substring(query.indexOf("token=") + 6);
                if (token.contains("&")) {
                    token = token.substring(0, token.indexOf("&"));
                }

                // 验证 token 并获取用户ID
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId != null) {
                    Long userId = Long.valueOf(loginId.toString());
                    attributes.put("userId", userId);
                    log.info("WebSocket 握手成功，用户ID: {}", userId);
                    return true;
                }
            }

            log.warn("WebSocket 握手失败：无效的 token");
            return false;
        } catch (Exception e) {
            log.error("WebSocket 握手异常", e);
            return false;
        }
    }

    /**
     * 握手后处理
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的处理
    }
}

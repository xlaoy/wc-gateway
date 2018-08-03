package com.xlaoy.wcgateway.security;

import com.xlaoy.common.support.JsonResponseWriter;
import com.xlaoy.wcgateway.support.ReactiveJsonResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Created by Administrator on 2018/7/25 0025.
 */
@Component
public class ApiServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException e) {

        Throwable throwable = e.getCause();

        if(throwable instanceof AccessDeniedException) {
            return Mono.empty();
        } else {
            logger.error("用户未登录,url[{}]", exchange.getRequest().getURI().getPath());

            return ReactiveJsonResponseWriter.response(exchange.getResponse())
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("用户未登录").print();
        }

    }
}

package com.xlaoy.wcgateway.security;

import com.xlaoy.wcgateway.support.ReactiveJsonResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Created by Administrator on 2018/8/1 0001.
 */
@Component
public class ApiServerAccessDeniedHandler implements ServerAccessDeniedHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        logger.error("用户权限不足,url[{}]", exchange.getRequest().getURI().getPath());

        return ReactiveJsonResponseWriter.response(exchange.getResponse())
                .status(HttpStatus.FORBIDDEN)
                .message("用户权限不足").print();
    }
}

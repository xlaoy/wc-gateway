package com.xlaoy.wcgateway.filter;

import com.xlaoy.common.config.ApiBasicAuthProperties;
import com.xlaoy.common.constants.SSOConstants;
import com.xlaoy.common.utils.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * Created by Administrator on 2018/7/25 0025.
 */
@Component
@EnableConfigurationProperties(ApiBasicAuthProperties.class)
public class ApiHeaderGlobalFilter implements GlobalFilter, Ordered {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApiBasicAuthProperties basicAuthProperties;

    @Override
    public int getOrder() {
        return 10010;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String guid = exchange.getAttribute(SSOConstants.GUID);
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(httpHeaders -> httpHeaders.remove(SSOConstants.JWT_TOKEN))
                .header(SSOConstants.GUID, guid)
                .header("Authorization", "Basic " + getAuthToken(url.getHost()))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private String getAuthToken(String serviceId) {
        try {
            ApiBasicAuthProperties.BasicAuthInfo basicAuthInfo = basicAuthProperties.getService(serviceId);
            logger.info("basicAuthInfo={}", JSONUtil.toJsonString(basicAuthInfo));
            byte[] bytes = (basicAuthInfo.getUsername() + ":" + basicAuthInfo.getPassword()).getBytes(StandardCharsets.UTF_8);
            String authToken = new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
            logger.info("authToken={}", JSONUtil.toJsonString(authToken));
            return authToken;
        }catch (Exception e) {
            throw new RuntimeException("Basic Auth 配置错误");
        }
    }

}

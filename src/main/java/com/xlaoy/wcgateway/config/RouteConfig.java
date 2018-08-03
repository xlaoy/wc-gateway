package com.xlaoy.wcgateway.config;

import com.xlaoy.wcgateway.hystrix.HystrixCommandController;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Created by Administrator on 2018/7/24 0024.
 */
@Component
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
               .route(r -> r.path("/api-user/**").filters(f ->
                      f.hystrix("", fallback())
                       .retry(retry())
                       .stripPrefix(1))
                       .uri("lb://XLAOY-SERVER")
               )
               .route(r -> r.path("/api-trade/**").filters(f ->
                      f.hystrix("", fallback())
                       .retry(retry())
                       .stripPrefix(1))
                       .uri("lb://XLAOY-SERVER")
               )
               .build();
    }

    private RetryGatewayFilterFactory.Retry retry() {
        return new RetryGatewayFilterFactory.Retry()
                .methods(HttpMethod.GET)
                .statuses(HttpStatus.NOT_FOUND, HttpStatus.BAD_GATEWAY)
                .retries(2);
    }

    private URI fallback() {
        return URI.create("forward:/" + HystrixCommandController.API_FALLBACK);
    }



}

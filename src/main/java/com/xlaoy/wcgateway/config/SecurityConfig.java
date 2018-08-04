package com.xlaoy.wcgateway.config;


import com.xlaoy.wcgateway.filter.JwtTokenWebFilter;
import com.xlaoy.wcgateway.security.ApiServerAccessDeniedHandler;
import com.xlaoy.wcgateway.security.ApiServerAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import com.xlaoy.wcgateway.security.ApiReactiveAuthorizationManager;
import org.springframework.security.web.server.authorization.ExceptionTranslationWebFilter;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;


@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private JwtTokenWebFilter jwtTokenWebFilter;
    @Autowired
    private ApiServerAuthenticationEntryPoint apiServerAuthenticationEntryPoint;
    @Autowired
    private ApiServerAccessDeniedHandler apiServerAccessDeniedHandler;
    @Autowired
    private ApiReactiveAuthorizationManager apiReactiveAuthorizationManager;


    @Bean
    public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) throws Exception {
        SecurityWebFilterChain filterChain = http.httpBasic().disable()
                    .csrf().disable()
                    .formLogin().disable()
                    .logout().disable()
                    .addFilterAt(jwtTokenWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                    .authorizeExchange()
                    .pathMatchers(HttpMethod.OPTIONS).permitAll()
                    .pathMatchers("/refesh_resource_keys").permitAll()
                    .anyExchange()
                    .access(apiReactiveAuthorizationManager)
                    .and()
                    .build();
        filterChain.getWebFilters().filter(filter -> filter instanceof ExceptionTranslationWebFilter).subscribe(filter -> {
            ExceptionTranslationWebFilter exceptionFilter = (ExceptionTranslationWebFilter)filter;
            exceptionFilter.setAuthenticationEntryPoint(apiServerAuthenticationEntryPoint);
            exceptionFilter.setAccessDeniedHandler(apiServerAccessDeniedHandler);
        });
        return filterChain;
    }

}


package com.xlaoy.wcgateway.security;

import com.xlaoy.common.constants.RedisHashName;
import com.xlaoy.common.constants.SSOConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ApiReactiveAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ReactiveRedisTemplate reactiveRedisTemplate;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {

        String url = context.getExchange().getRequest().getURI().getPath();

        List<String> urlPerList = this.getURLPermission(url);

        if(CollectionUtils.isEmpty(urlPerList)) {
            return Mono.just(new AuthorizationDecision(true));
        }

        return authentication
                .filter(a -> a.isAuthenticated())
                .flatMapIterable(a -> a.getAuthorities())
                .any(authority -> urlPerList.contains(authority.getAuthority()))
                .map(result -> new AuthorizationDecision(result))
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    /**
     *
     * @param url
     * @return
     */
    private List<String> getURLPermission(String url) {
        Object obj = reactiveRedisTemplate.opsForHash().get(RedisHashName.URL_PERMISSION, url).block();
        if(obj != null) {
            return (ArrayList<String>)obj;
        }
        return null;
    }

}

package com.xlaoy.wcgateway.filter;

import com.xlaoy.common.constants.RedisHashName;
import com.xlaoy.common.constants.SSOConstants;
import com.xlaoy.wcgateway.support.ReactiveJsonResponseWriter;
import com.xlaoy.common.utils.JSONUtil;
import com.xlaoy.wcgateway.exception.UserChangeException;
import com.xlaoy.wcgateway.security.JwtAuthenticationToken;
import com.xlaoy.wcgateway.security.LoginUser;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Administrator on 2018/7/25 0025.
 */
@Component
public class JwtTokenWebFilter implements WebFilter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${jwt.secret}")
    private String secret;
    @Autowired
    private UserDetailsChecker userChecker;
    @Autowired
    private ReactiveRedisTemplate reactiveRedisTemplate;

    private static final String FILTE_KEY = ".jwttokenfilter";
    private static final String FILTED = "jwttokenfilter_has_already_filtered";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        List<String> tokenList = exchange.getRequest().getHeaders().get(SSOConstants.JWT_TOKEN);
        if(CollectionUtils.isEmpty(tokenList)) {
            return chain.filter(exchange);
        } else {
            boolean canSetUser = exchange.getAttribute(FILTE_KEY) == null;
            if(canSetUser) {
                exchange.getAttributes().put(FILTE_KEY, FILTED);
                return setSecurityUser(exchange, chain, tokenList);
            } else {
                return chain.filter(exchange);
            }
        }
    }

    /**
     *
     * @param exchange
     * @param chain
     * @param tokenList
     * @return
     */
    private Mono<Void> setSecurityUser(ServerWebExchange exchange, WebFilterChain chain, List<String> tokenList) {
        LoginUser loginUser;
        try {
            loginUser = getLoginUser(tokenList);
            userChecker.check(loginUser);
        } catch (Exception exception) {
            logger.warn("设置SecurityUser异常", exception);
            ServerHttpResponse response = exchange.getResponse();
            if(exception instanceof UserChangeException) {
                return ReactiveJsonResponseWriter.response(response)
                        .status(HttpStatus.UNAUTHORIZED)
                        .message("用户需要重新登陆").print();
            } else {
                return ReactiveJsonResponseWriter.response(response)
                        .status(HttpStatus.BAD_REQUEST)
                        .message("用户异常").print();
            }
        }
        if(loginUser == null) {
            return chain.filter(exchange);
        } else {
            exchange.getAttributes().put(SSOConstants.GUID, loginUser.getGuid());
            JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(loginUser, loginUser.getGuid(), loginUser.getAuthorities());
            jwtAuthenticationToken.setDetails(loginUser);
            return chain.filter(exchange).subscriberContext(ReactiveSecurityContextHolder.withAuthentication(jwtAuthenticationToken));
        }
    }

    /**
     *
     * @param tokenList
     * @return
     */
    private LoginUser getLoginUser(List<String> tokenList) {

        logger.info("请求头信息：jwttoken={}", JSONUtil.toJsonString(tokenList));

        String token = tokenList.get(0);
        Claims claims;
        try {
            claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            logger.warn("jwttoken过期：{}", e.getMessage());
            return null;
        }

        LoginUser loginUser = new LoginUser();
        String guid = claims.get(SSOConstants.GUID, String.class);
        loginUser.setGuid(guid);

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Object obj = reactiveRedisTemplate.opsForHash().get(RedisHashName.USER_PERMISSION, guid).block();
        if(obj != null) {
            List<String> rolesArray = (ArrayList<String>)obj;
            for(String role : rolesArray) {
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                authorities.add(authority);
            }
        }

        /**reactiveRedisTemplate.opsForHash().get(RedisHashName.USER_PERMISSION, guid).subscribe(obj -> {
            //异步方式，redis数据没有返回之前，不会执行这里的代码
            if(obj != null) {
                List<String> rolesArray = (ArrayList<String>)obj;
                for(String role : rolesArray) {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                    authorities.add(authority);
                }
            }
        });
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }**/

        logger.info("用户信息：guid={},roles={}", guid, JSONUtil.toJsonString(authorities));

        loginUser.setAuthorities(authorities);

        return loginUser;
    }

}

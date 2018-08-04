package com.xlaoy.wcgateway.filter;

import com.xlaoy.common.constants.RedisHashName;
import com.xlaoy.common.constants.SSOConstants;
import com.xlaoy.common.utils.Java8TimeUtil;
import com.xlaoy.wcgateway.support.ReactiveJsonResponseWriter;
import com.xlaoy.common.utils.JSONUtil;
import com.xlaoy.wcgateway.exception.UserChangeException;
import com.xlaoy.wcgateway.security.JwtAuthenticationToken;
import com.xlaoy.wcgateway.security.LoginUser;
import io.jsonwebtoken.*;
import lombok.Data;
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

import java.text.SimpleDateFormat;
import java.util.*;

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
        LoginUser loginUser = null;
        try {
            Claims claims = this.getClaims(tokenList.get(0), exchange);
            if(claims != null) {
                loginUser = getLoginUser(claims);
                userChecker.check(loginUser);
            }
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

    private Claims getClaims(String token, ServerWebExchange exchange) {
        logger.info("请求头信息：jwttoken={}", token);
        Claims claims = null;
        try {
            claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            claims = this.checkExpirate(e, exchange);
        }
        return claims;
    }

    /**
     * 折半处理逻辑，用户真正的有效时长为jwt设置有效时常的两倍
     * 在给定的时间内如果用户过期则视为依然有效，重设jwttoken
     * @param e
     * @param exchange
     * @return
     */
    private Claims checkExpirate(ExpiredJwtException e, ServerWebExchange exchange) {
        Claims claims = e.getClaims();
        Date expiration = claims.getExpiration();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(expiration);
        calendar.add(Calendar.SECOND, 30 * 60);
        Date realExpir = calendar.getTime();
        Date now = new Date();
        if(now.after(realExpir)) {
            logger.warn("登录信息过期，guid={},expiration={}", claims.get(SSOConstants.GUID, String.class),
                    new SimpleDateFormat(Java8TimeUtil.YYYY_MM_DD_HH_MM_SS).format(realExpir));
            return null;
        }
        claims.setIssuedAt(now);
        claims.setExpiration(realExpir);
        Map<String, Object> map = new HashMap();
        map.put(SSOConstants.GUID, claims.get(SSOConstants.GUID, String.class));
        String newToken = Jwts.builder()
                .setClaims(map)
                .setIssuedAt(now)
                .setExpiration(realExpir)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
        exchange.getResponse().getHeaders().add(SSOConstants.JWT_TOKEN, newToken);
        return claims;
    }


    /**
     *
     * @param claims
     * @return
     */
    private LoginUser getLoginUser(Claims claims) {

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

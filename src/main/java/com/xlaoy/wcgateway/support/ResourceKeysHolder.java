package com.xlaoy.wcgateway.support;

import com.xlaoy.common.constants.RedisHashName;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by Administrator on 2018/8/4 0004.
 */
@Component
public class ResourceKeysHolder implements InitializingBean {

    @Autowired
    private ReactiveRedisTemplate reactiveRedisTemplate;

    private volatile List<String> resourceKeysHolder = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        this.refeshResourceKeys();
    }

    public void refeshResourceKeys() {
        List<String> list = new ArrayList<>();
        reactiveRedisTemplate.opsForHash().keys(RedisHashName.URL_PERMISSION).toIterable().forEach(url -> {
            list.add(url.toString());
        });
        resourceKeysHolder = list;
    }

    public List<String> getResourceKeysHolder() {
        return resourceKeysHolder;
    }
}

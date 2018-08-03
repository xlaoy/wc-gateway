package com.xlaoy.wcgateway.config;

import com.xlaoy.common.utils.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * Created by Administrator on 2018/8/1 0001.
 */
@Component
public class RedisConfig {

    @Autowired
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Bean
    public ReactiveRedisTemplate reactiveRedisTemplate() {

        RedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(JSONUtil.getObjectMapper());

        RedisSerializationContext serializationContext = RedisSerializationContext
                .newSerializationContext()
                .key(stringSerializer)
                .value(jsonRedisSerializer)
                .hashKey(stringSerializer)
                .hashValue(jsonRedisSerializer)
                .build();
        ReactiveRedisTemplate reactiveRedisTemplate = new ReactiveRedisTemplate(reactiveRedisConnectionFactory, serializationContext);
        return reactiveRedisTemplate;
    }

}

package com.xlaoy.wcgateway.support;

import com.xlaoy.common.exception.ExceptionResponse;
import com.xlaoy.common.utils.JSONUtil;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

/**
 * Created by Administrator on 2018/6/28 0028.
 */
public class ReactiveJsonResponseWriter {

    private ServerHttpResponse response;

    private ExceptionResponse exceptionResponse = new ExceptionResponse();

    public static ReactiveJsonResponseWriter response(ServerHttpResponse response) {
        ReactiveJsonResponseWriter writer = new ReactiveJsonResponseWriter();
        writer.setResponse(response);
        return writer;
    }

    private ReactiveJsonResponseWriter setResponse(ServerHttpResponse response) {
        this.response = response;
        return this;
    }

    public ReactiveJsonResponseWriter status(HttpStatus httpStatus) {
        response.setStatusCode(httpStatus);
        exceptionResponse.setCode(httpStatus.value());
        return this;
    }

    public ReactiveJsonResponseWriter message(String message) {
        exceptionResponse.setMessage(message);
        return this;
    }

    public Mono<Void> print() {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        DataBuffer buffer = response.bufferFactory().wrap(JSONUtil.toJsonString(exceptionResponse).getBytes(Charset.defaultCharset()));
        return response.writeWith(Mono.just(buffer)).doOnError( error -> DataBufferUtils.release(buffer));
    }
}

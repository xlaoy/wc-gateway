package com.xlaoy.wcgateway.config;

import com.xlaoy.common.constants.SSOConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.CorsRegistration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Created by Administrator on 2018/7/24 0024.
 */
@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

    @Bean
    public CorsWebFilter corsWebFilter() {
        WebCorsRegistration corsRegistration = new WebCorsRegistration("/**");
        corsRegistration.allowedOrigins(CorsConfiguration.ALL)
                .allowedMethods(
                        HttpMethod.PATCH.name(),
                        HttpMethod.HEAD.name(),
                        HttpMethod.OPTIONS.name(),
                        HttpMethod.GET.name(),
                        HttpMethod.HEAD.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.DELETE.name())
                .allowedHeaders(CorsConfiguration.ALL)
                .exposedHeaders(SSOConstants.JWT_TOKEN);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", corsRegistration.getCorsConfiguration());
        return new CorsWebFilter(source);
    }

    private static class WebCorsRegistration extends CorsRegistration {

        public WebCorsRegistration(String pathPattern) {
            super(pathPattern);
        }

        @Override
        protected CorsConfiguration getCorsConfiguration() {
            return super.getCorsConfiguration();
        }
    }
}

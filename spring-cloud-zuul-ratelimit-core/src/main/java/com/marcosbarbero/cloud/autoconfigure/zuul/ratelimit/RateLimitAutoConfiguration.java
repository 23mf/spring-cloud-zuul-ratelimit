/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit;

import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.*;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.repository.RedisRateLimiter;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.filters.RateLimitPostFilter;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.filters.RateLimitPreFilter;
import com.netflix.zuul.ZuulFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.util.UrlPathHelper;

import java.util.List;

import static com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties.PREFIX;

/**
 * @author Marcos Barbero
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true")
public class RateLimitAutoConfiguration {

    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    @Bean
    public ZuulFilter rateLimiterPreFilter(final RateLimiter rateLimiter,
                                           final RateLimitProperties properties,
                                           final RouteLocator routeLocator,
                                           final RateLimitKeyGenerator rateLimitKeyGenerator,
                                           final UserIDGenerator userIDGenerator) {
        valid(properties);
        return new RateLimitPreFilter(properties, routeLocator, urlPathHelper, rateLimiter,
                rateLimitKeyGenerator, userIDGenerator);
    }

    @Bean
    public ZuulFilter rateLimiterPostFilter(final RateLimiter rateLimiter,
                                            final RateLimitProperties properties,
                                            final RouteLocator routeLocator,
                                            final RateLimitKeyGenerator rateLimitKeyGenerator,
                                            final UserIDGenerator userIDGenerator) {
        valid(properties);
        return new RateLimitPostFilter(properties, routeLocator, urlPathHelper, rateLimiter,
                rateLimitKeyGenerator, userIDGenerator);
    }

    @Bean
    @ConditionalOnMissingBean(UserIDGenerator.class)
    public UserIDGenerator userIdGetter() {
        return new DefaultUserIDGenerator();
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitKeyGenerator.class)
    public RateLimitKeyGenerator ratelimitKeyGenerator(final RateLimitProperties properties,
                                                       final UserIDGenerator userIDGenerator) {
        valid(properties);
        return new DefaultRateLimitKeyGenerator(userIDGenerator, properties);
    }

    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnProperty(prefix = PREFIX, name = "repository", havingValue = "REDIS")
    public static class RedisConfiguration {

        @Bean("rateLimiterRedisTemplate")
        public StringRedisTemplate redisTemplate(final RedisConnectionFactory connectionFactory) {
            return new StringRedisTemplate(connectionFactory);
        }

        @Bean
        public RateLimiter redisRateLimiter(@Qualifier("rateLimiterRedisTemplate") final RedisTemplate redisTemplate) {
            return new RedisRateLimiter(redisTemplate);
        }
    }

    private void valid(RateLimitProperties properties) {
        List<RateLimitProperties.Policy> policies = properties.getNewPolicies();
        policies.forEach(this::valid);
    }

    private void valid(RateLimitProperties.Policy policy) {
        RateLimitProperties.HttpStatus httpStatus = policy.getHttpStatus();
        if (httpStatus != null) {
            if (httpStatus.getLimit() == null
                    || httpStatus.getStatuses() == null
                    || "".equals(httpStatus.getStatuses())) {
                throw new IllegalArgumentException("policy name : " + policy.getName() + " config error, " +
                        "http status field limit and statuses must not empty!");
            }
        }
    }

}

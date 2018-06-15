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

package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.repository;

import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.Rate;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.RateLimiter;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties.Policy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Marcos Barbero
 * @author Liel Chayoun
 */
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class RedisRateLimiter implements RateLimiter {

    private static final String QUOTA_SUFFIX = "-quota";

    private final RedisTemplate redisTemplate;

    @Override
    public Rate consume(final Policy policy, final String key, final Long requestTime, final int httpResponseStatus) {
        final Long refreshInterval = policy.getRefreshInterval();
        final Long quota = policy.getQuota() != null ? SECONDS.toMillis(policy.getQuota()) : null;
        final Rate rate =
                new Rate(key, policy.getLimit(), quota, policy.getHttpStatusLimit(), null, null);

        calcRemainingLimit(policy.getLimit(), refreshInterval, requestTime, key, rate);
        calcRemainingQuota(quota, refreshInterval, requestTime, key, rate);
        calcRemainingHttpStatuses(policy.getHttpStatus(),
                refreshInterval, requestTime, httpResponseStatus, key, rate);

        return rate;
    }

    private void calcRemainingLimit(Long limit, Long refreshInterval,
                                    Long requestTime, String key, Rate rate) {
        if (limit != null) {
            long usage = requestTime == null ? 1L : 0L;
            Long current = this.redisTemplate.boundValueOps(key).increment(usage);
            handleExpiration(key, refreshInterval, rate);
            rate.setRemaining(Math.max(-1, limit - current));
        }
    }

    private void calcRemainingQuota(Long quota, Long refreshInterval,
                                    Long requestTime, String key, Rate rate) {
        if (quota != null) {
            String quotaKey = key + QUOTA_SUFFIX;
            Long usage = requestTime != null ? requestTime : 0L;
            Long current = this.redisTemplate.boundValueOps(quotaKey).increment(usage);
            handleExpiration(quotaKey, refreshInterval, rate);
            rate.setRemainingQuota(Math.max(-1, quota - current));
        }
    }

    private void calcRemainingHttpStatuses(RateLimitProperties.HttpStatus httpStatus, Long refreshInterval,
                                           Long requestTime, int httpResponseStatus, String key, Rate rate) {
        if (httpStatus != null && httpStatus.getStatuses().contains(String.valueOf(httpResponseStatus))) {
            String quotaKey = key + "-" + httpStatus.getStatuses();
            long usage = requestTime == null ? 1L : 0L;
            Long current = this.redisTemplate.boundValueOps(quotaKey).increment(usage);
            handleExpiration(quotaKey, refreshInterval, rate);
            rate.setRemainingQuota(Math.max(-1, httpStatus.getLimit() - current));
        }
    }

    private void handleExpiration(String key, Long refreshInterval, Rate rate) {
        Long expire = this.redisTemplate.getExpire(key);
        if (expire == null || expire == -1) {
            this.redisTemplate.expire(key, refreshInterval, SECONDS);
            expire = refreshInterval;
        }
        rate.setReset(SECONDS.toMillis(expire));
    }
}

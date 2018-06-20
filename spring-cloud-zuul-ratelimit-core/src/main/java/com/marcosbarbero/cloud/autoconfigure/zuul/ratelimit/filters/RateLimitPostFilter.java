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

package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.filters;

import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.RateLimitKeyGenerator;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.RateLimiter;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.UserIDGenerator;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties;
import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.web.util.UrlPathHelper;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;

/**
 * @author Marcos Barbero
 * @author Liel Chayoun
 */
public class RateLimitPostFilter extends AbstractRateLimitFilter {

    private final RateLimiter rateLimiter;
    private final RateLimitKeyGenerator rateLimitKeyGenerator;

    public RateLimitPostFilter(final RateLimitProperties properties, final RouteLocator routeLocator,
                               final UrlPathHelper urlPathHelper, final RateLimiter rateLimiter,
                               final RateLimitKeyGenerator rateLimitKeyGenerator,
                               final UserIDGenerator userIDGenerator) {
        super(properties, routeLocator, urlPathHelper, userIDGenerator);
        this.rateLimiter = rateLimiter;
        this.rateLimitKeyGenerator = rateLimitKeyGenerator;
    }

    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_RESPONSE_FILTER_ORDER - 10;
    }

    /**
     * do post policy
     *
     * @param policy {@link RateLimitProperties.Policy}
     */
    @Override
    protected void doPolicy(RateLimitProperties.Policy policy) {
        final RequestContext ctx = RequestContext.getCurrentContext();
        int responseStatusCode = ctx.getResponseStatusCode();
        final Route route = route();

        final Long requestTime = (Long) ctx.get(REQUEST_CONSUME_TIME);
        final String key = rateLimitKeyGenerator.key(ctx, route, policy);
        rateLimiter.consume(policy, key, requestTime, responseStatusCode);
    }

}

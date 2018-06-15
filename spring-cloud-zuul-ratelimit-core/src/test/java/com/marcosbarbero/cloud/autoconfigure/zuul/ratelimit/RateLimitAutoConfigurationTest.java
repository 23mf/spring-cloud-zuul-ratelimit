package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit;

import com.ecwid.consul.v1.ConsulClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.RateLimiter;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.repository.RedisRateLimiter;
import com.netflix.zuul.ZuulFilter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.Map;

import static com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marcos Barbero
 * @since 2017-06-28
 */
public class RateLimitAutoConfigurationTest {

    private AnnotationConfigWebApplicationContext context;

    @Before
    public void setUp() {
        System.setProperty(PREFIX + ".enabled", "true");
        this.context = new AnnotationConfigWebApplicationContext();
        this.context.setServletContext(new MockServletContext());
        this.context.register(Conf.class);
        this.context.register(RateLimitAutoConfiguration.class);
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(PREFIX + ".repository");
    }

    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void testZuulFilters() {
        this.context.refresh();

        Map<String, ZuulFilter> zuulFilterMap = context.getBeansOfType(ZuulFilter.class);
        assertThat(zuulFilterMap.size()).isEqualTo(2);
        assertThat(zuulFilterMap.keySet()).containsExactly("rateLimiterPreFilter", "rateLimiterPostFilter");
    }

    @Test
    public void testRedisRateLimiterByProperty() {
        System.setProperty(PREFIX + ".repository", "REDIS");
        this.context.refresh();

        Assert.assertTrue(this.context.getBean(RateLimiter.class) instanceof RedisRateLimiter);
    }

    @Configuration
        public static class Conf {

        @Bean
        public RouteLocator routeLocator() {
            return Mockito.mock(RouteLocator.class);
        }

        @Bean
        public ConsulClient consulClient() {
            return Mockito.mock(ConsulClient.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            return Mockito.mock(ObjectMapper.class);
        }

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            return Mockito.mock(RedisConnectionFactory.class);
        }
    }
}

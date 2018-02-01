package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.filters;

import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AbstractRateLimitFilterTest {

    @Test
    public void doPolicy() {
    }

    @Test
    public void match() {
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy();
        HashMap<RateLimitProperties.Policy.Type, String> types = new HashMap();
        types.put(RateLimitProperties.Policy.Type.USER, "must-exist");
        policy.setTypes(types);
        Map<RateLimitProperties.Policy.Type, String> requestInfo = new HashMap<>();
        requestInfo.put(RateLimitProperties.Policy.Type.USER, null);
        boolean match = AbstractRateLimitFilter.match(policy, requestInfo);
        Assert.assertFalse(match);
    }

    @Test
    public void isMatch() {

    }
}
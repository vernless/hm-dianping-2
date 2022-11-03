package com.hmdp.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testGetAll(){

        List<String> list = stringRedisTemplate.opsForValue().multiGet(Arrays.asList("key1", "key2"));
    }

}

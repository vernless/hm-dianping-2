package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @Author 滨
 * @Date 2022/11/3 10:28
 * @Description: TODO
 * @Version 1.0
 */
public class SimpleRedisLock implements ILock{
    private StringRedisTemplate stringRedisTemplate;
    private String name;

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    private static final String key_prefix = "lock:";
    private static final String id_prefix = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long>  unlock_script;
    static {
        unlock_script = new DefaultRedisScript<>();
        unlock_script.setLocation(new ClassPathResource("unlock.lua"));
        unlock_script.setResultType(Long.class);
    }
    @Override
    public boolean tryLock(long timeoutSec) {
        //获取线程标识
        String threadId = id_prefix + Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                key_prefix + name, threadId, timeoutSec, TimeUnit.SECONDS);
        //防止自动拆箱空指针异常（如果success是null）
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        //释放锁
        //使用lua脚本
        stringRedisTemplate.execute(unlock_script,
                Collections.singletonList(key_prefix + name),
                id_prefix + Thread.currentThread().getId());
        /*stringRedisTemplate.delete(key_prefix + name);*/
    }
}

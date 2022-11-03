package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @Author 滨
 * @Date 2022/10/30 13:35
 * @Description: 封装Redis工具类
 * @Version 1.0
 */
@Slf4j
@Component
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long time, TimeUnit unit){
        //写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    //解决缓存穿透
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> classType, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //1. 从redis中查找缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2. 判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //3. 存在则直接返回
            return JSONUtil.toBean(json,classType);

        }
        //判断命中的是否是空值说明是“”
        if (json != null) {
            //是“”,返回一个错误信息
            return null;
        }
        //使用Function<ID, R>参数
        //4. 不存在，根据id查找数据库
        R r = dbFallback.apply(id);
        //5. 数据库中不存在，返回错误
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //6. 存在，写入redis
        this.set(key, r, time, unit);
        //7. 返回结果
        return r;
    }

    // 逻辑时间过期解决缓存击穿:这种要自己手动添加进入Redis，即热点业务
    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> classType, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //1. 从redis中查找缓存
        String Json = stringRedisTemplate.opsForValue().get(key);
        //2. 判断是否存在
        if (StrUtil.isBlank(Json)) {
            //3. redis中不存在则直接返回
            return null;
        }

        //4. 命中，需要先把JSON反序列化为对象
        RedisData redisData = JSONUtil.toBean(Json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), classType);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5. 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //5.1 未过期，直接返回店铺信息
            return r;
        }
        //5.2 已过期，需要重建缓存信息
        //6. 重建缓存
        //6.1 获取互斥锁
        String lock_key = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = trylock(lock_key);

        //6.2 判断是否成功获取锁
        if (isLock) {
            //todo 6.3 成功，开启一个线程实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //重建缓存
                    R r1 = dbFallback.apply(id);
                    //写入Redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unlock(lock_key);
                }
            });
        }

        //6.4 返回过期商铺信息
        return r;
    }

    //缓存重建的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR =
            Executors.newFixedThreadPool(10);



    private boolean trylock(String key){
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

}

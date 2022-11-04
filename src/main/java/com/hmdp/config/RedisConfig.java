package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author 滨
 * @Date 2022/11/4 12:38
 * @Description: TODO
 * @Version 1.0
 */
@Configuration
public class RedisConfig {
    @Bean
    public RedissonClient redissonClient(){
        //配置类
        Config cofig = new Config();
        //添加redis地址，这里添加了单点的地址，也可以使用
        // config.useClusterServers()添加集群地址
        cofig.useSingleServer().setAddress("redis://192.168.32.128:6379")
                .setPassword("51314290");
        //创建客户端
        return Redisson.create(cofig);
    }
}

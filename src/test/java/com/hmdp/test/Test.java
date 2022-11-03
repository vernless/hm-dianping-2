package com.hmdp.test;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisWorker;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author 滨
 * @Date 2022/10/26 18:49
 * @Description: TODO
 * @Version 1.0
 */
@SpringBootTest
public class Test {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IShopTypeService typeService;
    /*public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        Queue<Integer> order = new ArrayDeque<>();
        Queue<Integer> now = new ArrayDeque<>();
        for(int i = 0; i < n; i++){
            int t = sc.nextInt();
            order.add(t);
        }
        for(int i = 0; i < n; i++){
            int t = sc.nextInt();
            now.add(t);
        }
        int result = 0;
        while(!now.isEmpty()){
            int nowInt = now.poll();
            if(order.peek() == nowInt){
                order.poll();
            }else{
                order.remove(nowInt);
                result++;
            }
        }
        System.out.println(result);
    }*/

    @Resource
    private RedisWorker redisWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @org.junit.jupiter.api.Test
    void testRedisWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for(int i = 0; i < 100; i++){
                long id = redisWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();

        for(int i = 0; i < 300; i++){
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("耗时：" + (end - begin));
    }
}

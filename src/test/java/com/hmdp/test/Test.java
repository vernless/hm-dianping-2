package com.hmdp.test;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IShopService;
import com.hmdp.service.IShopTypeService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.RegexUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.query;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * @Author 滨
 * @Date 2022/10/26 18:49
 * @Description: TODO
 * @Version 1.0
 */
@SpringBootTest
public class Test {
/*
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IShopTypeService typeService;
    */
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
    }*//*


    @Resource
    private IUserService userService;

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

    @org.junit.jupiter.api.Test
    void testUser(){
        Long id = 2L;
        User user = userService.getById(id);
        System.out.println(user);
    }

    @Resource
    private IShopService shopservice;
    @org.junit.jupiter.api.Test
    void testShop(){
        Shop shop = shopservice.getById(14);
        System.out.println(shop);
    }


    @Resource
    private UserMapper userMapper;
    @org.junit.jupiter.api.Test
    void testUserMapper(){
        int i = Integer.MIN_VALUE;
        User user = userMapper.selectById(1012);
        System.out.println(user);
    }
*/

    @org.junit.jupiter.api.Test
    void Test(){
        int[] arr = {1,2,3,1,2,3,2,2};
        Map<Integer, Integer> map = new HashMap<>();
        int maxLength = 0;
        int temp = 0;
        for(int i = 0; i < arr.length; i++){
            if(!map.containsKey(arr[i])){
                map.put(arr[i], i);
                maxLength = Math.max(maxLength, i - temp + 1);
                System.out.println("== " +maxLength);
            }else{
                maxLength = Math.max(maxLength, i - map.get(arr[i]));
                System.out.println("!= "+maxLength);
                temp = i + 1;
                map.put(arr[i], i);
            }
        }

    }
}

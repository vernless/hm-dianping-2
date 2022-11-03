package com.hmdp.utils;

/**
 * @Author 滨
 * @Date 2022/11/3 10:25
 * @Description: TODO
 * @Version 1.0
 */
public interface ILock {
    boolean tryLock(long timeoutSec);

    void unlock();
}

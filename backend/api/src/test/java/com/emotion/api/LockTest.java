package com.emotion.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
public class LockTest {


    private int a = 0;

    private synchronized void add(String name) {
        System.out.println(name+"开始");
        a++;
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(name+":"+a);
    }

    @Test
    public void test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        executorService.submit(() -> {
            System.out.println("a");
            add("a");
        });

        executorService.submit(() -> {
            System.out.println("b");
            add("b");
        });

        executorService.submit(() -> {
            System.out.println("c");
            add("c");
        });

        executorService.submit(() -> {
            System.out.println("d");
            add("d");
        });

        // 关闭线程池，不再接受新的任务
        executorService.shutdown();
        // 等待所有已提交的任务完成，最长等待时间为1分钟
        if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
            executorService.shutdownNow(); // 如果超时则强制关闭线程池
        }
    }
}

package com.emotion.api.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class JVMMemoryUsage {

    @Autowired
    private DingTalkMsg dingTalkMsg;
    /**
     * 输出内存占用情况
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void memoryUsage() {
        try {
            // 获取当前 JVM 进程 ID
            String pid = getProcessId();

            // 执行 jmap -histo 命令
            Process process = Runtime.getRuntime().exec("jmap -histo " + pid);

            // 读取命令输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (count <= 10) {
                    output.append(line).append("\n");
                }
                count++;
            }
            dingTalkMsg.sendMsgToDingTalk(output.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取当前 JVM 进程 ID
    private static String getProcessId() {
        // 获取当前 JVM 进程 ID
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    public static void main(String[] args) {
        System.out.println(getProcessId());
    }
}

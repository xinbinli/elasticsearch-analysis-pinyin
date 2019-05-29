package org.elasticsearch.index.dic;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 监控任务
 *
 * @author lixinbin
 * @create 2019-05-28 13:42
 **/

public class MonitorTask {

    //创建一个核心线程数为1的线程池，用于存放监控热更新的线程
    private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

    private static final String fileName = "/location.txt";

    public static synchronized void initial() {
        //读取本地文件中的远程文件地址
        InputStream in = MonitorTask.class.getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String location = null;
        try {
            location = reader.readLine();
        } catch (Exception ex) {
            throw new RuntimeException("read location.xml error.", ex);
        } finally {
            try {
                reader.close();
            } catch (Exception ignored) {
            }
        }
        //启动线程
        pool.scheduleAtFixedRate(new Monitor(location), 10, 20000, TimeUnit.MILLISECONDS);
    }


}

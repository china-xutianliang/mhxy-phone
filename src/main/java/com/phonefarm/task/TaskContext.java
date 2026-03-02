package com.phonefarm.task;

import com.phonefarm.config.DeviceConfig;

import java.util.function.Consumer;

/**
 * 任务运行时上下文，供 GameTask 内部使用。
 */
public class TaskContext {

    private final DeviceConfig config;
    private final Consumer<String> logger;
    private volatile boolean stopped;

    public TaskContext(DeviceConfig config, Consumer<String> logger) {
        this.config = config;
        this.logger = logger;
    }

    public DeviceConfig getConfig()  { return config; }

    public boolean isStopped()       { return stopped; }
    public void stop()               { this.stopped = true; }

    public void log(String msg) {
        if (logger != null) logger.accept(msg);
    }

    public void checkStop() throws InterruptedException {
        if (stopped) throw new InterruptedException("任务已停止");
    }

    public void sleep(long ms) throws InterruptedException {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            if (stopped) throw new InterruptedException("任务已停止");
            Thread.sleep(Math.min(200, end - System.currentTimeMillis()));
        }
    }
}

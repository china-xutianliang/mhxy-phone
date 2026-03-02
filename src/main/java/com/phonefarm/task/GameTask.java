package com.phonefarm.task;

/**
 * 自动化任务接口。每个具体任务实现此接口。
 */
public interface GameTask {

    /** 任务显示名称 */
    String name();

    /**
     * 执行任务。
     * 实现应定期检查 {@code ctx.isStopped()} 以支持中途停止。
     */
    void execute(TaskContext ctx) throws Exception;
}

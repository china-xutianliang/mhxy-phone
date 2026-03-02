package com.phonefarm.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 任务执行器：在独立线程中运行 GameTask，支持 start/stop。
 */
public class TaskRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);

    public enum State { IDLE, RUNNING, STOPPING }

    private final TaskContext context;
    private final Consumer<State> stateCallback;
    private volatile State state = State.IDLE;
    private Thread taskThread;
    private GameTask currentTask;

    public TaskRunner(TaskContext context, Consumer<State> stateCallback) {
        this.context = context;
        this.stateCallback = stateCallback;
    }

    public State getState() { return state; }

    public void start(GameTask task) {
        if (state != State.IDLE) {
            log.warn("TaskRunner 不在 IDLE 状态，无法启动");
            return;
        }
        this.currentTask = task;
        state = State.RUNNING;
        notifyState();

        taskThread = new Thread(() -> {
            context.log("任务开始: " + task.name());
            try {
                task.execute(context);
                context.log("任务完成: " + task.name());
            } catch (InterruptedException e) {
                context.log("任务已停止: " + task.name());
            } catch (Exception e) {
                context.log("任务异常: " + e.getMessage());
                log.error("任务执行异常: {}", task.name(), e);
            } finally {
                state = State.IDLE;
                currentTask = null;
                notifyState();
            }
        }, "Task-" + task.name());
        taskThread.setDaemon(true);
        taskThread.start();
    }

    public void stop() {
        if (state != State.RUNNING) return;
        state = State.STOPPING;
        notifyState();
        context.stop();
        if (taskThread != null) {
            taskThread.interrupt();
            try { taskThread.join(5000); } catch (InterruptedException ignored) {}
        }
    }

    public String getCurrentTaskName() {
        GameTask t = currentTask;
        return t != null ? t.name() : null;
    }

    private void notifyState() {
        if (stateCallback != null) {
            try { stateCallback.accept(state); } catch (Exception ignored) {}
        }
    }
}

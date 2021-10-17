package com.nullpo.utils

import org.jline.terminal.Size
import org.jline.terminal.impl.jna.linux.LinuxNativePty

import java.sql.Time
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Created with IntelliJ IDEA 2021.1.
 * date: 2021-10-16 0:36
 * author: nullpo
 * description: 
 */
class Cursor {

    /**
     * linux控制台.
     */
    private static volatile LinuxNativePty PTY

    /**
     * linux控制台尺寸.
     */
    private static Size SIZE

    /**
     * 维护线程对应的进度map.
     */
    private static volatile TreeMap<String, BarNode> MULTI_BAR = new TreeMap<>()

    /**
     * 上次打印的行数.
     */
    private static int OLD_LINES

    /**
     * 节点序列.
     */
    private static AtomicInteger ORDER = new AtomicInteger(0)

    /**
     * 定时器执行.
     */
    private static Timer timer

    /**
     * 定时任务是否已启动
     */
    private static volatile boolean taskStarted = false

    /**
     * 图像刷新周期.
     */
    private static long refreshTime = 100

    /**
     * 读写锁，保证新加节点时，不会打乱map的遍历.
     */
    private static ReadWriteLock rw = new ReentrantReadWriteLock()
    private static Lock r = rw.readLock()
    private static Lock w = rw.writeLock()

    /*------------------- PUBLIC INTERFACE -------------------*/

    static void open() {
        if (null == PTY) {
            synchronized (Cursor.class) {
                if (null == PTY) {
                    PTY = LinuxNativePty.current()
                }
            }
        }
        OLD_LINES = 0
        ORDER.set(0)
        timer = new Timer()
    }

    static void close() {
        MULTI_BAR.clear()
        timer.cancel()
    }

    static void bar(String name, Double progress) {
        if (!taskStarted) {
            synchronized (Cursor.class) {
                if (!taskStarted) {
                    taskStarted = true
                    timer.schedule(new GraphRefresh(), 0, refreshTime)
                }
            }
        }
        updateBar(name, progress)
    }

    /*------------------- PRIVATE TOOLS -------------------*/

    /**
     * 更新map.
     */
    private static void updateBar(name, progress) {
        progress = progress > 1.0 ? 1.0 : progress

        BarNode node
        if (node = MULTI_BAR[name]) {
            node.progress = progress
        } else {
            node = new BarNode(order: ORDER.getAndIncrement(), name: name, progress: progress)
            writeLock { MULTI_BAR.put(name, node) }
        }
    }

    /**
     * 打印map.
     */
    private static void show() {
        SIZE = PTY.size

        resetCursor()
        readLock {
            def graph = MULTI_BAR.values().sort()*.shape.join('\n')
            if (graph) {
                println graph
            }
        }
        OLD_LINES = MULTI_BAR.size()
    }

    /**
     * 根据OLD_LINES重置光标.
     */
    private static void resetCursor() {
        if (OLD_LINES) {
            print("\033[${OLD_LINES}A")
        }
    }

    /**
     * 定时刷新图像.
     */
    private static class GraphRefresh extends TimerTask {
        @Override
        void run() {
            show()
        }
    }

    /**
     * 进度条封装.
     */
    private static class BarNode implements Comparable<BarNode> {

        int order
        String name
        Double progress

        /**
         * 获取形状.
         */
        private String getShape() {
            String format = '\r%s:[%s] %6.2f%%'
            int remainCol = SIZE.columns - name.size() - 11

            String.format(format, name, progressGraph(progress, remainCol), progress * 100.0)
        }

        private static String progressGraph(progress, col) {
            int block = (progress * col) as int
            int blank = col - block

            def builder = new StringBuilder()
            block.times { builder.append('■') }
            blank.times { builder.append(' ') }
            builder.toString()
        }

        @Override
        int compareTo(BarNode node) {
            return this.order - node.order
        }
    }

    private static void readLock(Closure closure) {
        r.lock()
        try {
            closure.call()
        } finally {
            r.unlock()
        }
    }

    private static void writeLock(Closure closure) {
        w.lock()
        try {
            closure.call()
        } finally {
            w.unlock()
        }
    }

}

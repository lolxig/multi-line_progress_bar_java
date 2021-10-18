package com.nullpo.utils

import org.jline.terminal.Size
import org.jline.terminal.impl.jna.linux.LinuxNativePty

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
        //系统检测
        if (!System.getProperty('os.name').contains('Linux')) {
            throw new RuntimeException('仅支持Linux平台，不支持Windows平台！')
        }

        //获取当前linux控制台
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
        updateBar(name, progress)
        //若打印任务没启动，则以双重检查的方式启动它
        if (!taskStarted) {
            synchronized (Cursor.class) {
                if (!taskStarted) {
                    taskStarted = true
                    timer.schedule(new GraphRefresh(), 0, refreshTime)
                }
            }
        }
    }

    /*------------------- PRIVATE TOOLS -------------------*/

    /**
     * 更新map.
     */
    private static void updateBar(name, progress) {
        //进度只能是[0,1]的区间
        progress = Math.max(0.0, Math.min(progress, 1.0))

        //若map里存在，则更新；若不存在，则锁住map，然后添加节点
        BarNode node
        if (node = MULTI_BAR[name]) {
            node.setProgress(progress)
        } else {
            node = new BarNode(order: ORDER.getAndIncrement(), name: name, progress: progress,
                    startTime: System.currentTimeMillis())
            writeLock { MULTI_BAR.put(name, node) }
        }
    }

    /**
     * 定时刷新图像.
     */
    private static class GraphRefresh extends TimerTask {

        /**
         * 打印map.
         */
        private static void show() {
            SIZE = PTY.size

            resetCursor()
            def graph
            readLock {
                graph = MULTI_BAR.values()*.shape.join('\n')
            }
            if (graph) {
                println graph
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

        @Override
        void run() {
            show()
        }

    }

    /**
     * 进度条封装.
     */
    private static class BarNode implements Comparable<BarNode> {

        /**
         * 进度条停止计算标志.
         */
        boolean stop
        /**
         * 节点入map的顺序.
         */
        int order
        /**
         * 节点名字.
         */
        String name
        /**
         * 节点进度.
         */
        Double progress
        /**
         * 节点加入时间，默认是加入即开始.
         */
        long startTime
        /**
         * 节点字符串缓存.
         */
        String cache
        /**
         * 节点更新标志.
         */
        boolean update
        /**
         * 节点进度条字符串缓存.
         */
        String progressCache

        /**
         * 进度条格式.
         */
        private static final String format = '\r%s:[%s] %6.2f%% '

        /**
         * 获取形状.
         */
        private String getShape() {
            if (stop) {
                return cache
            }

            String tg = timeGraph(startTime)
            if (update) {
                int remainCol = SIZE.columns - name.size() - 12 - tg.size()
                synchronized (this) {
                    progressCache = String.format(format, name, progressGraph(progress, remainCol), progress * 100.0)
                }
                update = false
            }
            cache = progressCache + tg
        }

        void setProgress(double progress) {
            update = true
            synchronized (this) {
                if (progress >= 1.0) {
                    /* 进度满的时候，必须先设置进度为1，然后刷新缓存，再设置stop为true，顺序绝对不能乱 */
                    this.progress = 1.0
                    shape
                    stop = true
                } else {
                    this.progress = progress
                }
            }
        }

        private static String timeGraph(st) {
            long sec = (long) ((System.currentTimeMillis() - st) / 1000)

            int h, m, s
            def builder = new StringBuilder()
            if (h = (int) (sec / 3600)) {
                builder.append(h).append('h')
            }
            if (m = (int) ((sec % 3600) / 60)) {
                builder.append(m).append('m')
            }
            s = (sec % 3600) % 60
            builder.append(s).append('s').toString()
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

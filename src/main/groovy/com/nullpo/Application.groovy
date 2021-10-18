package com.nullpo

import com.nullpo.utils.Cursor
import groovy.util.logging.Slf4j

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors


/**
 * Created with IntelliJ IDEA 2021.1.
 * date: 2021-10-15 22:55
 * author: nullpo
 * description: 
 */
@Slf4j
class Application {

    static void main(String[] args) {
        Cursor.open()

        def n = 1999
        def pool = Executors.newFixedThreadPool(3)
        def latch = new CountDownLatch(n)

        n.times {i ->
            pool.submit {
                def name = 'n' + i
                double progress = 0.0
                def r = new Random()

                while (progress <= 1.0) {
                    progress += (r.nextInt(10)) / 10000.0
                    Cursor.bar(name, progress)
                    sleep(10)
                }
                latch.countDown()
            }
        }
        latch.await()
        pool.shutdown()

        Cursor.close()
    }

}

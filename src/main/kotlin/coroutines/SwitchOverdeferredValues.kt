package coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select

/**
 * @author Allen
 * @date : 2018/12/12
 * 修改时间：
 * 修改备注：
 */
fun CoroutineScope.switchMapDeferreds(input: ReceiveChannel<Deferred<String>>) = produce<String> {
    var current = input.receive() // start with first received deferred value
    while (isActive) { // loop while not cancelled/closed
        val next = select<Deferred<String>?> { // return next deferred value from this select or null
            input.onReceiveOrNull { update ->
                update // replaces next value to wait
            }
            current.onAwait { value ->
                send(value) // send value that current deferred has produced
                input.receiveOrNull() // and use the next deferred from the input channel
            }
        }
        if (next == null) {
            println("Channel was closed")
            break // out of loop
        } else {
            current = next
        }
    }
}
fun CoroutineScope.asyncString(str: String, time: Long) = async {
    delay(time)
    str
}

fun main(args: Array<String>) {
    runBlocking<Unit> {
        //sampleStart
        val chan = Channel<Deferred<String>>() // the channel for test
        launch { // launch printing coroutine
            for (s in switchMapDeferreds(chan))
                println(s) // print each received string
        }
        chan.send(asyncString("BEGIN", 100))
        delay(200) // enough time for "BEGIN" to be produced
        chan.send(asyncString("Slow", 500))
        delay(100) // not enough time to produce slow
        chan.send(asyncString("Replace", 100))
        delay(500) // give it time before the last one
        chan.send(asyncString("END", 500))
        delay(1000) // give it time to process
        chan.close() // close the channel ...
        delay(500) // and wait some time to let it finish
//sampleEnd
    }
}
package com.ariy.retrolauncher

import android.os.CancellationSignal
import android.os.OperationCanceledException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive

/** A typed checkpoint also lets SAF cancel a blocked Android provider query. */
internal class RomScanCancellationCheck(private val context: CoroutineContext) : () -> Unit {
    override fun invoke() { context.ensureActive() }
}

private val romQueryWatchdog = Executors.newScheduledThreadPool(2) { runnable ->
    Thread(runnable, "rom-query-cancellation").apply { isDaemon = true }
}

internal fun <T> withRomQueryCancellation(
    check: () -> Unit, block: (CancellationSignal?) -> T
): T {
    if (check !is RomScanCancellationCheck) return block(null)
    check()
    val signal = CancellationSignal()
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
    // The scanner may be blocked in a provider on its IO thread. Check its Job from
    // a bounded shared watchdog rather than waiting for that query to return.
    val watcher = romQueryWatchdog.scheduleAtFixedRate({
        try {
            check()
            if (System.nanoTime() >= deadline) signal.cancel()
        } catch (_: CancellationException) { signal.cancel() }
    }, 100, 100, TimeUnit.MILLISECONDS)
    try {
        return block(signal)
    } catch (error: OperationCanceledException) {
        check() // Job cancellation remains cancellation, not a platform error.
        throw IllegalStateException("ROM storage provider query timed out or was cancelled; retry the folder scan", error)
    } finally {
        watcher.cancel(false)
    }
}
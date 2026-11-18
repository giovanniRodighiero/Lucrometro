package com.lucrometro.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One request to the source at a time (PRD), shared by every repository. A coroutine lock, not the source's own
 * monitor: waiting for it can be cancelled, so closing the add sheet while a refresh holds the source abandons the
 * check at once instead of waiting for the refresh's request. The blocking call runs on [io] and is interruptible:
 * cancelling the caller aborts it instead of leaving it to hold the source until it times out.
 */
class SourceGate(private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val lock = Mutex()

    /** Runs the blocking [request] in its turn. */
    suspend fun <T> request(request: () -> T): T = lock.withLock { runInterruptible(io, block = request) }

    /**
     * Like [request], but once the turn comes [reuse] is asked first (the request before this one may have fetched the
     * same thing): its answer is returned instead, if it has one. The flag says whether [request] ran.
     */
    suspend fun <T : Any> requestUnless(reuse: () -> T?, request: () -> T): Pair<T, Boolean> = lock.withLock {
        reuse()?.let { it to false } ?: (runInterruptible(io, block = request) to true)
    }
}

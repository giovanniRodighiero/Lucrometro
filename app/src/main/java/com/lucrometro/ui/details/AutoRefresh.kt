package com.lucrometro.ui.details

import kotlinx.coroutines.delay

/** Auto-refresh on the details screen (decision 0035): whether it runs, and when it last checked (epoch millis). */
data class AutoRefresh(val on: Boolean = false, val checkedAt: Long? = null)

/** 30 s, counted from the end of the previous fetch: 2 requests a minute for one open ticker (decision 0035). */
const val AUTO_REFRESH_MILLIS = 30_000L

/**
 * The auto-refresh loop: waits [intervalMillis], then [fetch]es, again and again, one request at a time. Returns when
 * the market is no longer open before a fetch, or when a fetch fails (no retries: a 429 must stop it). Cancelling the
 * caller stops it too.
 */
suspend fun pollWhileOpen(intervalMillis: Long, isOpen: () -> Boolean, fetch: suspend () -> Boolean) {
    while (true) {
        delay(intervalMillis)
        if (!isOpen() || !fetch()) return
    }
}

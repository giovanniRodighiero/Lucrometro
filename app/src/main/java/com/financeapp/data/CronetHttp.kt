package com.financeapp.data

import android.content.Context
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class HttpResult(val status: Int, val body: String, val setCookies: List<String>)

/** Minimal blocking GET on top of Cronet (Chrome's network stack). */
class CronetHttp(context: Context) {
    private val engine: CronetEngine = CronetEngine.Builder(context.applicationContext)
        .enableHttp2(true)
        .enableQuic(true)
        .setUserAgent(USER_AGENT)
        .build()
    private val executor = Executors.newFixedThreadPool(2)

    fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResult {
        val latch = CountDownLatch(1)
        val out = ByteArrayOutputStream()
        val cookies = mutableListOf<String>()
        var status = -1
        var error: Throwable? = null

        val callback = object : UrlRequest.Callback() {
            override fun onRedirectReceived(r: UrlRequest, i: UrlResponseInfo, to: String) {
                collect(i)
                r.followRedirect()
            }

            override fun onResponseStarted(r: UrlRequest, i: UrlResponseInfo) {
                status = i.httpStatusCode
                collect(i)
                r.read(ByteBuffer.allocateDirect(16 * 1024))
            }

            override fun onReadCompleted(r: UrlRequest, i: UrlResponseInfo, b: ByteBuffer) {
                b.flip()
                val bytes = ByteArray(b.remaining()).also { b.get(it) }
                out.write(bytes)
                b.clear()
                r.read(b)
            }

            override fun onSucceeded(r: UrlRequest, i: UrlResponseInfo) = latch.countDown()

            override fun onFailed(r: UrlRequest, i: UrlResponseInfo?, e: CronetException) {
                error = e
                latch.countDown()
            }

            private fun collect(i: UrlResponseInfo) {
                i.allHeadersAsList
                    .filter { it.key.equals("set-cookie", ignoreCase = true) }
                    .forEach { cookies += it.value }
            }
        }

        val builder = engine.newUrlRequestBuilder(url, callback, executor)
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        builder.build().start()

        if (!latch.await(20, TimeUnit.SECONDS)) error("Timeout fetching $url")
        error?.let { throw it }
        return HttpResult(status, out.toString("UTF-8"), cookies)
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    }
}

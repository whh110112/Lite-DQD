package com.whh11.dqdlite.util

import android.webkit.JavascriptInterface
import android.webkit.WebView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 评论数据桥。
 *
 * 为什么需要原生桥：
 * WebView 里加载的是 `n.dongqiudi.com`，而评论接口在 `api.dongqiudi.com`。
 * 页面内 `fetch()` 属于跨域请求，响应没有 `Access-Control-Allow-Origin`，
 * 浏览器会在 CORS 预检阶段直接拒绝 —— 表现就是"评论永远加载不出来"，
 * 控制台只留一行 `load comment err`。
 *
 * 由原生侧发起请求不受同源策略约束，拿到 JSON 后再交回页面渲染。
 *
 * 用法（JS 侧）：
 *   window.DqdNative.loadComments(articleId)          // 原生异步拉取
 *   window.__dqdOnComments(articleId, jsonString)     // 原生回填，JS 需先注册
 */
class CommentBridge(private val webView: WebView) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /** 同一篇文章的评论只拉一次，翻页由页面内的"查看更多"另行处理 */
    private val cache = HashMap<String, String>()

    @JavascriptInterface
    fun loadComments(articleId: String) {
        if (articleId.isBlank()) return

        cache[articleId]?.let { cached ->
            deliver(articleId, cached)
            return
        }

        Thread {
            val json = runCatching { fetch(articleId) }.getOrDefault("{}")
            if (json != "{}") cache[articleId] = json
            deliver(articleId, json)
        }.apply { isDaemon = true }.start()
    }

    private fun fetch(articleId: String): String {
        val url = "https://api.dongqiudi.com/v2/article/$articleId/comment?page=1&size=50"
        val req = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"
            )
            .header("Referer", "https://n.dongqiudi.com/")
            .header("Accept", "application/json")
            .build()
        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) "{}" else resp.body?.string().orEmpty()
        }
    }

    /** 回主线程执行 JS，把 JSON 作为字符串字面量传进去，避免引号/换行破坏语法 */
    private fun deliver(articleId: String, json: String) {
        val aid = JSONObject.quote(articleId)
        val payload = JSONObject.quote(json)
        val js = "window.__dqdOnComments && window.__dqdOnComments($aid, $payload);"
        webView.post { webView.evaluateJavascript(js, null) }
    }
}

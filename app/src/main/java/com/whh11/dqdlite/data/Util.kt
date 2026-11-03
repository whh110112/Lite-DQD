package com.whh11.dqdlite.data

import android.content.Context
import android.net.ConnectivityManager
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 秒级时间戳 -> "8分钟前" / "昨天" / "11-03" */
fun relTime(epochSeconds: Long, now: Long = System.currentTimeMillis() / 1000): String {
    if (epochSeconds <= 0) return ""
    val diff = now - epochSeconds
    return when {
        diff < 0 -> formatDate(epochSeconds)
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 172800 -> "昨天"
        diff < 604800 -> "${diff / 86400}天前"
        else -> formatDate(epochSeconds)
    }
}

private fun formatDate(epochSeconds: Long): String =
    SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(epochSeconds * 1000))

fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

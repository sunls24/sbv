package dev.sunls24.sbv.util

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun String.toast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this, duration).show()
}

fun Int.toast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, context.getText(this), duration).show()
}

fun <T> SnapshotStateList<T>.swapList(newList: List<T>) {
    clear()
    addAll(newList)
}

fun Date.formatPubTimeString(): String {
    val calendar = Calendar.getInstance()
    calendar.time = this
    val year = calendar.get(Calendar.YEAR)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    val formatter = if (year == currentYear) {
        SimpleDateFormat("MM月dd日HH:mm", Locale.CHINA)
    } else {
        SimpleDateFormat("yyyy年MM月dd日HH:mm", Locale.CHINA)
    }
    return formatter.format(this)
}

fun Long.formatHourMinSec(): String {
    if (this < 0L) return "..."

    val s = this / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60

    val sb = StringBuilder(8)

    if (h > 0) {
        if (h < 10) sb.append('0')
        sb.append(h).append(':')
    }

    if (m < 10) sb.append('0')
    sb.append(m).append(':')

    if (sec < 10) sb.append('0')
    sb.append(sec)

    return sb.toString()
}

/**
 * 改进的请求焦点的方法，失败后等待 100ms 后重试
 */
fun FocusRequester.requestFocus(scope: CoroutineScope) {
    // scope.launch(Dispatchers.Default) {
    scope.launch( Dispatchers.Main) {
        runCatching {
            requestFocus()
        }.onFailure {
            delay(100)
            runCatching { requestFocus() }
        }
    }
}

fun String.removeHtmlTags(): String = HtmlCompat.fromHtml(
    this, HtmlCompat.FROM_HTML_MODE_LEGACY
).toString()

fun KeyEvent.isKeyDown(): Boolean = type == KeyEventType.KeyDown
fun KeyEvent.isDpadRight(): Boolean = key == Key.DirectionRight

private fun Long.toWanValueString(): String =
    if (this < 10_000) toString()
    else "${(this / 1000) / 10f}万"

fun Int?.toWanString(): String = this?.toLong()?.toWanValueString().orEmpty()

fun Long?.toWanString(): String = this?.toWanValueString().orEmpty()

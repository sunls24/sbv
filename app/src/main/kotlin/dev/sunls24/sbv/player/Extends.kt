package dev.sunls24.sbv.player

import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.formatMinSec(): String {
    return if (this < 0L) {
        "..."
    } else {
        String.format(
            Locale.ROOT,
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) -
                    TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(this)
                    )
        )
    }
}

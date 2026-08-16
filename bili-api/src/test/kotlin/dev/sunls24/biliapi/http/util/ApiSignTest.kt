package dev.sunls24.biliapi.http.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiSignTest {
    private val imgKey = "7cd084941338484aae1ad9425b84077c"
    private val subKey = "4932caff0ff746eab6f01bf08b70ac45"

    @Test
    fun `signs sorted WBI parameters`() {
        val signature = signWbiParameters(
            parameters = mapOf("foo" to "114", "bar" to "514", "baz" to "1919810"),
            imgKey = imgKey,
            subKey = subKey,
            wts = 1702204169
        )

        assertEquals("6149fdadf571698ca7e6a567265cd0ee", signature)
    }

    @Test
    fun `filters before encoding unicode spaces and special characters`() {
        val signature = signWbiParameters(
            parameters = mapOf("keyword" to "中文 空格!'()*~", "page" to "1"),
            imgKey = imgKey,
            subKey = subKey,
            wts = 1702204169
        )

        assertEquals("96d2ecb3daac6950e95163574e17202b", signature)
    }
}

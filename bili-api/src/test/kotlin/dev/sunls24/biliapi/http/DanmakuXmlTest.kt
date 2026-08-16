package dev.sunls24.biliapi.http

import kotlin.test.Test
import kotlin.test.assertEquals

class DanmakuXmlTest {
    @Test
    fun `parses metadata and danmaku with streaming parser`() {
        val response = BiliHttpApi.parseDanmakuXml(
            """<?xml version="1.0" encoding="UTF-8"?>
                <i>
                    <chatserver>chat.example.com</chatserver>
                    <chatid>123</chatid>
                    <maxlimit>1000</maxlimit>
                    <state>0</state>
                    <real_name>0</real_name>
                    <source>k-v</source>
                    <d p="1.5,1,25,16777215,1700000000,0,hash,42,11">中文 &amp; text</d>
                </i>
            """.trimIndent().byteInputStream(),
            transform = { it.dmid to it.text }
        )

        assertEquals("chat.example.com", response.chatserver)
        assertEquals(123, response.chatId)
        assertEquals(1000, response.maxLimit)
        assertEquals(42L to "中文 & text", response.data.single())
    }
}

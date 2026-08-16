package dev.sunls24.biliapi.http.entity.user

import dev.sunls24.biliapi.entity.user.UpProfile
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserCardTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decode and map public user card`() {
        val data = json.decodeFromString<WebUserCardData>(
            """{
                "card": {
                    "mid": "2",
                    "name": "UP主",
                    "face": "https://example.com/avatar.jpg",
                    "sign": "个人签名",
                    "attention": 428,
                    "level_info": { "current_level": 6 },
                    "Official": { "role": 2, "title": "官方认证" }
                },
                "following": false,
                "follower": 1421557,
                "archive_count": 44,
                "like_num": 5058746
            }"""
        )

        val profile = UpProfile.fromWebUserCardData(99, data)

        assertEquals(2, profile.mid)
        assertEquals("UP主", profile.name)
        assertEquals("个人签名", profile.sign)
        assertEquals(6, profile.level)
        assertEquals("官方认证", profile.officialTitle)
        assertEquals(428, profile.followingCount)
        assertEquals(1421557, profile.followerCount)
        assertEquals(44, profile.archiveCount)
        assertEquals(5058746, profile.likeCount)
    }

    @Test
    fun `map optional user card fields`() {
        val data = json.decodeFromString<WebUserCardData>(
            """{
                "card": {
                    "name": "普通用户",
                    "face": "",
                    "sign": "",
                    "attention": 0,
                    "Official": { "role": 0, "title": "" }
                }
            }"""
        )

        val profile = UpProfile.fromWebUserCardData(123, data)

        assertEquals(123, profile.mid)
        assertNull(profile.level)
        assertNull(profile.officialTitle)
        assertEquals(0, profile.followerCount)
        assertEquals(0, profile.archiveCount)
    }
}

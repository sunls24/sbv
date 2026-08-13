package dev.sunls24.biliapi.http

import dev.sunls24.biliapi.http.entity.AuthFailureException
import dev.sunls24.biliapi.http.entity.BiliResponse
import dev.sunls24.biliapi.http.entity.RiskControlException
import dev.sunls24.biliapi.http.entity.login.qr.AppQRLoginData
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApiContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `normalize top-level app token`() {
        val data = json.decodeFromString<AppQRLoginData>(
            """{"access_token":"access","refresh_token":"refresh","expires_in":3600}"""
        )

        assertEquals("access", data.normalizedToken().accessToken)
        assertEquals("refresh", data.normalizedToken().refreshToken)
        assertEquals(3600, data.normalizedToken().accessTokenExpiresIn)
    }

    @Test
    fun `normalize nested app token and refresh expiry`() {
        val data = json.decodeFromString<AppQRLoginData>(
            """{
                "token_info":{"access_token":"access","refresh_token":"refresh","expires_in":3600},
                "refresh_token_info":{"expires_in":2592000}
            }"""
        )

        assertEquals("access", data.normalizedToken().accessToken)
        assertEquals("refresh", data.normalizedToken().refreshToken)
        assertEquals(3600, data.normalizedToken().accessTokenExpiresIn)
        assertEquals(2592000, data.normalizedToken().refreshTokenExpiresIn)
    }

    @Test
    fun `classify auth and risk control codes`() {
        assertFailsWith<AuthFailureException> {
            BiliResponse<Unit>(code = -658, message = "expired").getResponseData()
        }
        for (code in listOf(-352, -412, -509, -799)) {
            assertFailsWith<RiskControlException> {
                BiliResponse<Unit>(code = code, message = "risk").getResponseData()
            }
        }
    }
}

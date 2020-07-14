package com.xama.client

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.util.*

data class Credentials(
        val accessToken: String,
        val tenantId: UUID,
        val userAgent: String = "xama-xero-client=${UUID.randomUUID()}"
) {
    fun toHttpHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        headers.set(HttpHeaders.USER_AGENT, userAgent)
        headers.set("xero-tenant-id", tenantId.toString())
        return headers
    }
}

class CustomHttpException(val title: String, val detail: String, val status: HttpStatus)  : RuntimeException(){
    override fun toString(): String {
        return "CustomHttpException(title='$title', detail='$detail', status=$status)"
    }
}
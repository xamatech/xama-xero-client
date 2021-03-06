package com.xama.client

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.client.ClientHttpResponse


internal class ExtendedResource(val name: String, byteArray: ByteArray) : ByteArrayResource(byteArray) {
    override fun getFilename(): String = name
}


internal fun handleProblem(response: ClientHttpResponse) {
    val body = response.body.bufferedReader().use { it.readText() }
    throw CustomHttpException(
            response.statusText,
            body,
            response.statusCode
    )
}
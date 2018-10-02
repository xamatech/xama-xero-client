package com.xama.client

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.client.ClientHttpResponse
import java.util.*



enum class AppType { PUBLIC, PRIVATE, PARTNER }

data class Credentials(val token: String, val tokenSecret: String)

data class Config(val consumerKey: String,
                  val consumerSecret : String,
                  val credentialsProvider: () -> Credentials,
                  val appType: AppType = AppType.PUBLIC,
                  val userAgent: String = "userAgent-${UUID.randomUUID()}"
//  "PrivateKeyCert" :  "/xero_certs/public_privatekey.pfx",
//                  "PrivateKeyPassword" :  ""
)


internal class ExtendedResource(val name: String, byteArray: ByteArray) : ByteArrayResource(byteArray) {
    override fun getFilename(): String = name
}


internal fun handleProblem(response: ClientHttpResponse) {
    val body = response.body.bufferedReader().use { it.readText() }
    // TOOD
    throw Exception(
            response.statusText + " " +
                    body + " " +
                    response.statusCode
    )
}
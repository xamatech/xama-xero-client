package com.xama.client

import org.springframework.http.HttpStatus
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

class CustomHttpException(val title: String, val detail: String, val status: HttpStatus)  : RuntimeException(){
    override fun toString(): String {
        return "CustomHttpException(title='$title', detail='$detail', status=$status)"
    }
}
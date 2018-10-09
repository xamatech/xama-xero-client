package com.xama.client

import org.springframework.http.HttpStatus
import java.io.InputStream
import java.net.URL
import java.security.KeyStore
import java.security.PrivateKey
import java.util.*

enum class AppType { PUBLIC, PRIVATE, PARTNER }

data class Credentials(val token: String, val tokenSecret: String)

class Config private constructor(val consumerKey: String,
                                 val consumerSecret : String,
                                 val credentialsProvider: (() -> Credentials)?,
                                 val appType: AppType,
                                 val privateKeyCertStream : InputStream? = null,
                                 val privateKeyPassword: String? = null,
                                 val userAgent: String
) {
    val signatureMethod: String = if(appType == AppType.PUBLIC) "HMAC-SHA1" else "RSA-SHA1"
    val algorithm: String = if(appType == AppType.PUBLIC) "HmacSHA1" else "SHA1withRSA"



    var oauthKey: PrivateKey? = if(appType != AppType.PUBLIC){
        requireNotNull(privateKeyCertStream){ "keyCertStream must not be null" }

        val oauthKeyStore: KeyStore = KeyStore.getInstance("PKCS12")
        val privateKeyPasswordChars = privateKeyPassword!!.toCharArray()
        oauthKeyStore.load(privateKeyCertStream, privateKeyPasswordChars)

        val alias = oauthKeyStore.aliases().asSequence().filter { oauthKeyStore.isKeyEntry(it) }.first()
        requireNotNull(alias){
            "could not find suitable alias in givent private key certificate with given password"
        }

        oauthKeyStore.getKey(alias, privateKeyPasswordChars) as PrivateKey
    }
    else null

    companion object {

        fun getPublicAppConfig(consumerKey: String,
                               consumerSecret : String,
                               credentialsProvider: () -> Credentials,
                               userAgent: String = "userAgent-${UUID.randomUUID()}"): Config {
            return Config(
                    appType = AppType.PUBLIC,
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    credentialsProvider = credentialsProvider,
                    userAgent = userAgent
            )
        }


        fun getPublicAppConfig(consumerKey: String,
                               consumerSecret : String,
                               userAgent: String = "userAgent-${UUID.randomUUID()}"): Config {
            return Config(
                    appType = AppType.PUBLIC,
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    credentialsProvider = null,
                    userAgent = userAgent
            )
        }


        fun getPartnerAppConfig(consumerKey: String,
                                consumerSecret : String,
                                urlToPrivateKeyCert: URL,
                                privateKeyPassword: String = "",
                                userAgent: String = "userAgent-${UUID.randomUUID()}"): Config {
            return getPartnerAppConfig(
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    privateKeyCertStream = urlToPrivateKeyCert.openStream(),
                    privateKeyPassword = privateKeyPassword,
                    userAgent = userAgent
            )
        }


        fun getPartnerAppConfig(consumerKey: String,
                                consumerSecret : String,
                                privateKeyCertStream: InputStream,
                                privateKeyPassword: String = "",
                                userAgent: String = "userAgent-${UUID.randomUUID()}"): Config {
            return Config(
                    appType = AppType.PARTNER,
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    credentialsProvider = null,
                    privateKeyCertStream = privateKeyCertStream,
                    privateKeyPassword = privateKeyPassword,
                    userAgent = userAgent
            )
        }


        fun getPrivateAppConfig(consumerKey: String,
                                consumerSecret : String,
                                urlToPrivateKeyCert: URL,
                                privateKeyPassword: String = "",
                                userAgent: String = "userAgent-${UUID.randomUUID()}"): Config {
            return getPrivateAppConfig(
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    privateKeyCertStream = urlToPrivateKeyCert.openStream(),
                    privateKeyPassword = privateKeyPassword,
                    userAgent = userAgent
            )
        }


        fun getPrivateAppConfig(consumerKey: String,
                                consumerSecret : String,
                                privateKeyCertStream: InputStream,
                                privateKeyPassword: String = "",
                                userAgent: String = "userAgent-${UUID.randomUUID()}"): Config {
            return Config(
                    appType = AppType.PRIVATE,
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    credentialsProvider = {
                        Credentials(
                                token=consumerKey,
                                tokenSecret=consumerSecret
                        )
                    },
                    privateKeyCertStream = privateKeyCertStream,
                    privateKeyPassword = privateKeyPassword,
                    userAgent = userAgent
            )
        }
    }
}

class CustomHttpException(val title: String, val detail: String, val status: HttpStatus)  : RuntimeException(){
    override fun toString(): String {
        return "CustomHttpException(title='$title', detail='$detail', status=$status)"
    }
}
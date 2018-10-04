package com.xama.client

import org.springframework.http.HttpStatus
import java.security.KeyStore
import java.security.PrivateKey
import java.util.*

enum class AppType { PUBLIC, PRIVATE, PARTNER }

data class Credentials(val token: String, val tokenSecret: String)

class Config private constructor(val consumerKey: String,
                                 val consumerSecret : String,
                                 val credentialsProvider: () -> Credentials,
                                 val appType: AppType,
                                 val privateKeyCert: String? = null,
                                 val privateKeyPassword: String? = null,
                                 val userAgent: String
) {

    val signatureMethod: String = if(appType == AppType.PUBLIC) "HMAC-SHA1" else "RSA-SHA1"
    val algorithm: String = if(appType == AppType.PUBLIC) "HmacSHA1" else "SHA1withRSA"



    var oauthKey: PrivateKey? = if(appType != AppType.PUBLIC){
        val oauthKeyStore: KeyStore = KeyStore.getInstance("PKCS12")
        val privateKeyCertStream = Config::class.java.getResourceAsStream(privateKeyCert)
        oauthKeyStore.load(privateKeyCertStream, privateKeyPassword!!.toCharArray())

        val alias = oauthKeyStore.aliases().asSequence().filter { oauthKeyStore.isKeyEntry(it) }.first()
        requireNotNull(alias){
            "could not find suitable alias in [privateKeyCert=$privateKeyCert] with given password"
        }

        oauthKeyStore.getKey(alias, privateKeyPassword!!.toCharArray()) as PrivateKey
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

        fun getPartnerAppConfig(consumerKey: String,
                                consumerSecret : String,
                                privateKeyCert: String,
                                privateKeyPassword: String = "",
                                credentialsProvider: () -> Credentials,
                                userAgent: String = "userAgent-${UUID.randomUUID()}"): Config {
            return Config(
                    appType = AppType.PARTNER,
                    consumerKey = consumerKey,
                    consumerSecret = consumerSecret,
                    credentialsProvider = credentialsProvider,
                    privateKeyCert = privateKeyCert,
                    privateKeyPassword = privateKeyPassword,
                    userAgent = userAgent
            )
        }


        fun getPrivateAppConfig(consumerKey: String,
                                consumerSecret : String,
                                privateKeyCert: String ,
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
                    privateKeyCert = privateKeyCert,
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
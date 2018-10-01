package com.xama.client

import com.google.api.client.util.StringUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


private const val ENCODING = "UTF-8"

private val RANDOM = SecureRandom()
private fun nonce(random: SecureRandom = RANDOM): String = java.lang.Long.toHexString(Math.abs(random.nextLong()))

private fun computeTimestamp(): String = (System.currentTimeMillis() / 1000L).toString()

/**
 * see https://oauth.net/core/1.0a/#signing_process
 */
internal fun oauthHeaders(config: Config,
                          httpMethod: HttpMethod,
                          requestPath: String,
                          parameters: List<Pair<String, String>> = listOf()
): HttpHeaders {
    val credentials = config.credentialsProvider()
    val oauthHeaders = oauthHeaders(config = config, credentials = credentials)

    val allParameters = oauthHeaders.toMutableList()
    allParameters.addAll(parameters)

    val normalisedParams = allParameters
            .sortedBy {  p -> p.first + p.second} // TODO better way of sorting for 1st and then for value?
            .map{ pair -> pair.first to pair.second }

    val signature = computeSignature(
            config = config,
            credentials = credentials,
            httpMethod = httpMethod,
            requestPath = requestPath,
            normalisedParams = normalisedParams
    )

    val joinedOAuthHeaderParams = oauthHeaders
            .joinToString(
                    transform = {p -> "${p.first}=\"${URLEncoder.encode(p.second, ENCODING)}\""},
                    separator = ", "
            )

    return HttpHeaders().apply {
        add(
                HttpHeaders.AUTHORIZATION,
                "OAuth $joinedOAuthHeaderParams, oauth_signature=\"${URLEncoder.encode(signature, ENCODING)}\""
        )
    }
}


private fun oauthHeaders(config: Config, credentials: Credentials) = listOf(
        "oauth_consumer_key" to config.consumerKey,
        "oauth_nonce" to nonce(),
        "oauth_timestamp" to computeTimestamp(),
        "oauth_signature_method" to "HMAC-SHA1", // config.signer.signatureMethod, // TODO
        "oauth_token" to credentials.token,
        "oauth_version" to "1.0"
)


private fun computeSignature(config: Config,
                             credentials: Credentials,
                             httpMethod: HttpMethod,
                             requestPath: String,
                             normalisedParams: List<Pair<String, String>>): String {

    val concatenatedParams = normalisedParams
            .joinToString(
                    transform = {p -> "${p.first}=${p.second}"},
                    separator = "&"
            )

    val signatureStringBuilder = StringBuilder()
    signatureStringBuilder
            .append(httpMethod.name).append('&')
            .append(URLEncoder.encode(requestPath, ENCODING)).append('&')
            .append(URLEncoder.encode(concatenatedParams, ENCODING))

    val signatureString = signatureStringBuilder.toString()

    return hmacSha1(signatureString, credentials, config) // TODO
}


private fun hmacSha1(signatureBaseString: String, credentials: Credentials, config: Config): String {

    val keyBuilder = StringBuilder().apply {
        append(URLEncoder.encode(config.consumerSecret, ENCODING))
        append('&')
        append(URLEncoder.encode(credentials.tokenSecret, ENCODING))
    }

    val secretKey = SecretKeySpec(keyBuilder.toString().toByteArray(Charsets.UTF_8), "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(secretKey)

    return Base64.getEncoder().encodeToString(mac.doFinal(StringUtils.getBytesUtf8(signatureBaseString)))
}
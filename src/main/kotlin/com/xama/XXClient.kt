package com.xama

import com.google.api.client.util.StringUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.zalando.riptide.*
import org.zalando.riptide.Route.call
import org.zalando.riptide.capture.Capture
import org.zalando.riptide.problem.ProblemRoute.problemHandling
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.CompletableFuture
import org.zalando.riptide.Http
import org.springframework.core.io.ByteArrayResource
import org.springframework.util.LinkedMultiValueMap
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


private val RANDOM = SecureRandom()
fun nonce(random: SecureRandom = RANDOM): String = java.lang.Long.toHexString(Math.abs(random.nextLong()))

fun computeTimestamp(): String = (System.currentTimeMillis() / 1000L).toString()


/**
 * see https://developer.xero.com/documentation/files-api/files
*/

data class UserDto(val id: UUID, val name: String?, val firstName: String?, val lastName: String?, val fullName: String?)
data class FileDto(val id: UUID, val folderId: UUID?, val size: Int, val createdDateUtc: String, val updatedDateUtc: String, val user: UserDto?)
data class GetFilesResponseDto(val totalCount: Int, val page: Int, val perPage: Int, val items: List<FileDto>)




enum class AppType { PUBLIC, PRIVATE, PARTNER}

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


class FilesClient (val http: Http, val config: Config){

    companion object {
        const val BASE_URL = "https://api.xero.com/files.xro/1.0/files"
        const val ENCODING = "UTF-8"
    }


    //Bindings.on(HttpStatus.Series.SUCCESSFUL).call({ response: ClientHttpResponse, reader: MessageReader -> println(response.body.bufferedReader().use { it.readText() }) }),

    fun getFiles(): CompletableFuture<GetFilesResponseDto> {
        val capture = Capture.empty<GetFilesResponseDto>()
        val future = http.get(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                // .ifModifiedSince() TODO
                .header(HttpHeaders.USER_AGENT, config.userAgent)
                .headers(oauthHeaders(
                        httpMethod = HttpMethod.GET,
                        requestPath = BASE_URL
                        // TODO query params
                ))
                .dispatch(Navigators.series(),
                        Bindings.on(HttpStatus.Series.SUCCESSFUL).call(GetFilesResponseDto::class.java, capture),
                        Bindings.anySeries().call(problemHandling(call { p -> handleProblem(p) }))
                )
                .thenApply(capture);

        return future
    }


    fun postFile(fileName: String, fileUrl: URL): CompletableFuture<FileDto> {

        val multiValueMap = LinkedMultiValueMap<String, Any>()
        multiValueMap[fileName] =  ExtendedResource(fileName, fileUrl.readBytes())

        val capture = Capture.empty<FileDto>()
        val future = http.post(BASE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                // .ifModifiedSince() TODO
                .header(HttpHeaders.USER_AGENT, config.userAgent)
                .headers(oauthHeaders(
                        httpMethod = HttpMethod.POST,
                        requestPath = BASE_URL
                ))
                .body(multiValueMap)
                .dispatch(Navigators.series(),
                        Bindings.on(HttpStatus.Series.SUCCESSFUL).call(FileDto::class.java, capture),
                        Bindings.anySeries().call(problemHandling(call { p -> handleProblem(p) }))
                )
                .thenApply(capture);

        return future
    }



    /**
     * see https://oauth.net/core/1.0a/#signing_process
     */
    fun oauthHeaders(httpMethod: HttpMethod,
                     requestPath: String,
                     parameters: List<Pair<String, String>> = listOf()
    ): HttpHeaders {
        val credentials = config.credentialsProvider()
        val oauthHeaders = oauthHeaders(credentials = credentials)

        val allParameters = oauthHeaders.toMutableList()
        allParameters.addAll(parameters)

        val normalisedParams = allParameters
                .sortedBy {  p -> p.first + p.second} // TODO better way of sorting for 1st and then for value?
                .map{ pair -> pair.first to pair.second }

        val signature = computeSignature(
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


    private fun oauthHeaders(credentials: Credentials) = listOf(
            "oauth_consumer_key" to config.consumerKey,
            "oauth_nonce" to nonce(),
            "oauth_timestamp" to computeTimestamp(),
            "oauth_signature_method" to "HMAC-SHA1", // config.signer.signatureMethod, // TODO
            "oauth_token" to credentials.token,
            "oauth_version" to "1.0"
    )


    private fun computeSignature(credentials: Credentials,
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


    fun hmacSha1(signatureBaseString: String, credentials: Credentials, config: Config): String {

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


    private fun handleProblem(response: ClientHttpResponse) {
        val body = response.body.bufferedReader().use { it.readText() }

        // TOOD
        throw Exception(
                response.statusText + " " +
                body + " " +
                response.statusCode
        )
    }
}

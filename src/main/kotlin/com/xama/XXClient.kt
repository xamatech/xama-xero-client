package com.xama

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.api.client.auth.oauth.OAuthHmacSigner
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
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
import org.zalando.riptide.OriginalStackTracePlugin
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.zalando.riptide.Http
import java.util.concurrent.TimeUnit
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.multipart.MultipartFile
import org.zalando.logbook.Logbook
import org.zalando.logbook.RawRequestFilter
import org.zalando.logbook.StreamHttpLogWriter
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory
import java.io.File
import java.net.URI
import java.net.URL


private val RANDOM = SecureRandom()
fun nonce(random: SecureRandom = RANDOM): String = java.lang.Long.toHexString(Math.abs(random.nextLong()))

fun computeTimestamp(): String = (System.currentTimeMillis() / 1000L).toString()


/**
 * see https://developer.xero.com/documentation/files-api/files
*/

data class UserDto(val id: UUID, val name: String?, val firstNam: String?, val lastName: String?, val fullName: String?)
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





class FilesClient (val http: Http, val config: Config){

    companion object {
        const val BASE_URL = "https://api.xero.com/files.xro/1.0/files"
        const val ENCODING = "UTF-8"
    }




    fun getFiles(): CompletableFuture<GetFilesResponseDto> {
        val capture = Capture.empty<GetFilesResponseDto>()
        val future = http.get(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                // .ifModifiedSince() TODO
                //.header(HttpHeaders.USER_AGENT, config.userAgent)
                .headers(headers(
                        httpMethod = HttpMethod.GET,
                        requestPath = BASE_URL
                        // TODO query params
                ))
                .dispatch(Navigators.series(),
                       // Bindings.on(HttpStatus.Series.SUCCESSFUL).call({ response: ClientHttpResponse, reader: MessageReader -> println(response.body.bufferedReader().use { it.readText() }) }),
                        Bindings.on(HttpStatus.Series.SUCCESSFUL).call(GetFilesResponseDto::class.java, capture),
                        Bindings.anySeries().call(problemHandling(call { p -> handleProblem(p) }))
                )
                .thenApply(capture);

        return future
    }


    class ExtendedResource(byteArray: ByteArray) : ByteArrayResource(byteArray) {
       override fun getFilename(): String = "test.jpg"
    }

    fun postFile(fileUrl: URL): CompletableFuture<FileDto> {

        val multiValueMap = LinkedMultiValueMap<String, Any>()
        multiValueMap["testben.jpg"] = ExtendedResource (fileUrl.readBytes()) // TODO

        val capture = Capture.empty<FileDto>()
        val future = http.post(BASE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                // .ifModifiedSince() TODO
                //.header(HttpHeaders.USER_AGENT, config.userAgent)
                .headers(headers(
                        httpMethod = HttpMethod.POST,
                        requestPath = BASE_URL,
                        parameters = listOf(
                          //      Pair("filename", "test.jpg"),
                          //      Pair("name", "file")
                        )
                       // parameters = listOf(Pair(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE))
                        // TODO query params
                ))
                .body(multiValueMap)
                .dispatch(Navigators.series(),
                        //Bindings.on(HttpStatus.Series.SUCCESSFUL).call({ response: ClientHttpResponse, reader: MessageReader -> println(response.body.bufferedReader().use { it.readText() }) }),
                        Bindings.on(HttpStatus.Series.SUCCESSFUL).call(FileDto::class.java, capture),
                        Bindings.anySeries().call(problemHandling(call { p -> handleProblem(p) }))
                )
                .thenApply(capture);

        return future
    }



    /**
     * see https://oauth.net/core/1.0a/#signing_process
     */
    fun headers(httpMethod: HttpMethod,
                requestPath: String,
                parameters: List<Pair<String, String>> = listOf()
    ): HttpHeaders {

        val before = System.currentTimeMillis()

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


        val headers = HttpHeaders()

        val preparedOAuthParams = oauthHeaders
                .joinToString(
                        transform = {p -> "${p.first}=\"${URLEncoder.encode(p.second, ENCODING)}\""},
                        separator = ", "
                )

        headers.add(
                HttpHeaders.AUTHORIZATION,
                "OAuth $preparedOAuthParams, oauth_signature=\"${URLEncoder.encode(signature, ENCODING)}\""
        )


        val after = System.currentTimeMillis()
        println("header prep---> ${after - before}")
        println("header --> $headers")

        return headers
    }

    private fun oauthHeaders(credentials: Credentials) = listOf(
            "oauth_consumer_key" to config.consumerKey,
            "oauth_nonce" to nonce(),
            "oauth_timestamp" to computeTimestamp(),
            "oauth_signature_method" to "HMAC-SHA1", // config.signer.signatureMethod,
            "oauth_token" to credentials.token,
            "oauth_version" to "1.0" // TODO
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

        println("signatureString: $signatureString")

        val signer = OAuthHmacSigner()
        signer.tokenSharedSecret = credentials.tokenSecret
        signer.clientSharedSecret = config.consumerSecret

        return  signer.computeSignature(signatureString)
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

fun main(args: Array<String>): Unit {


    val logbook = Logbook.builder()
            .clearBodyFilters()
            .clearHeaderFilters()
            .clearRawRequestFilters()
            .rawRequestFilter(RawRequestFilter.none())
            .clearRawResponseFilters()
            .clearRequestFilters()
            .writer(StreamHttpLogWriter(System.err))
            .build()

    val httpClient = HttpClientBuilder.create()
            // TODO configure client here
            .addInterceptorFirst(LogbookHttpRequestInterceptor(logbook))
            .build()

    val executor = ConcurrentTaskExecutor()


    val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder().modulesToInstall(
            KotlinModule())
            .createXmlMapper(false)
            .propertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE)
            .build()


    val http = Http.builder()
            //.requestFactory(HttpComponentsAsyncClientHttpRequestFactory())
            .requestFactory(RestAsyncClientHttpRequestFactory(httpClient, executor))
            .converter(MappingJackson2HttpMessageConverter(objectMapper))
            .converter(FormHttpMessageConverter())
            .plugin(OriginalStackTracePlugin())
            .build()



    val config = Config(
            appType = AppType.PUBLIC,
            userAgent = "",
            consumerKey = "",
            consumerSecret = "",
            credentialsProvider = {
                Credentials(
                        token = "",
                        tokenSecret = ""
                )
            }
    )



    val client = FilesClient(http = http, config = config)
/*
    val before1 = System.currentTimeMillis()
    var future1 = client.getFiles()
    var result1 = future1.get(10L, TimeUnit.SECONDS);
    println(System.currentTimeMillis() - before1)
    println(result1)

    println("--------")
*/
    val future = client.postFile(URL("file:///tmp/test_image.jpeg"))
    val result = future.get(60L, TimeUnit.SECONDS);
    println(result)
}

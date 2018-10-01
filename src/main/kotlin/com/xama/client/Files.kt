package com.xama.client

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.zalando.riptide.Bindings
import org.zalando.riptide.Http
import org.zalando.riptide.Navigators
import org.zalando.riptide.Route
import org.zalando.riptide.capture.Capture
import org.zalando.riptide.problem.ProblemRoute
import java.net.URL
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * see https://developer.xero.com/documentation/files-api/files
 */
class Files private constructor(){
    data class UserDto(val id: UUID, val name: String?, val firstName: String?, val lastName: String?, val fullName: String?)
    data class FileDto(val id: UUID, val folderId: UUID?, val size: Int, val createdDateUtc: String, val updatedDateUtc: String, val user: UserDto?)
    data class GetFilesResponseDto(val totalCount: Int, val page: Int, val perPage: Int, val items: List<FileDto>)

    class Client (val http: Http, val config: Config){
        companion object {
            const val BASE_URL = "https://api.xero.com/files.xro/1.0/files"
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
                            config = config,
                            httpMethod = HttpMethod.GET,
                            requestPath = BASE_URL
                            // TODO query params
                    ))
                    .dispatch(Navigators.series(),
                            Bindings.on(HttpStatus.Series.SUCCESSFUL).call(GetFilesResponseDto::class.java, capture),
                            Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                    )
                    .thenApply(capture);

            return future
        }


        fun postFile(fileName: String, fileUrl: URL): CompletableFuture<FileDto> {

            val multiValueMap = LinkedMultiValueMap<String, Any>()
            multiValueMap[fileName] = ExtendedResource(fileName, fileUrl.readBytes())

            val capture = Capture.empty<FileDto>()
            val future = http.post(BASE_URL)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    // .ifModifiedSince() TODO
                    .header(HttpHeaders.USER_AGENT, config.userAgent)
                    .headers(oauthHeaders(
                            config = config,
                            httpMethod = HttpMethod.POST,
                            requestPath = BASE_URL
                    ))
                    .body(multiValueMap)
                    .dispatch(Navigators.series(),
                            Bindings.on(HttpStatus.Series.SUCCESSFUL).call(FileDto::class.java, capture),
                            Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                    )
                    .thenApply(capture);

            return future
        }
    }

}
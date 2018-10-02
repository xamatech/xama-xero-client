package com.xama.client.files

import com.xama.client.Config
import com.xama.client.ExtendedResource
import com.xama.client.handleProblem
import com.xama.client.oauthHeaders
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
    data class FileDto(val id: UUID, val folderId: UUID?, val name: String, val mimeType: String, val size: Int, val createdDateUtc: String, val updatedDateUtc: String, val user: UserDto?)
    data class GetFilesResponseDto(val totalCount: Int, val page: Int, val perPage: Int, val items: List<FileDto>)

    private data class FileChangeDto(val name: String?, val folderId: UUID?)

    class Client (val http: Http, val config: Config){
        companion object {
            const val BASE_URL = "https://api.xero.com/files.xro/1.0/files"
        }

        private fun fileUri(fileId: UUID) = "$BASE_URL/$fileId"

        //Bindings.on(HttpStatus.Series.SUCCESSFUL).call({ response: ClientHttpResponse, reader: MessageReader -> println(response.body.bufferedReader().use { it.readText() }) }),

        fun getFiles(): CompletableFuture<GetFilesResponseDto> {
            val capture = Capture.empty<GetFilesResponseDto>()
            return http.get(BASE_URL)
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
        }


        fun getFile(fileId: UUID): CompletableFuture<FileDto> {
            val requestUrl = fileUri(fileId)
            val capture = Capture.empty<FileDto>()

            return http.get(requestUrl)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.USER_AGENT, config.userAgent)
                    .headers(oauthHeaders(
                            config = config,
                            httpMethod = HttpMethod.GET,
                            requestPath = requestUrl
                    ))
                    .dispatch(Navigators.series(),
                            Bindings.on(HttpStatus.Series.SUCCESSFUL).call(FileDto::class.java, capture),
                            Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                    )
                    .thenApply(capture);
        }


        fun uploadFile(fileName: String, fileUrl: URL): CompletableFuture<FileDto> {
            val multiValueMap = LinkedMultiValueMap<String, Any>()
            multiValueMap[fileName] = ExtendedResource(fileName, fileUrl.readBytes())

            val capture = Capture.empty<FileDto>()
            return http.post(BASE_URL)
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
        }


        fun changeFile(fileId: UUID, newFileName: String? = null, newFolderId: UUID? = null): CompletableFuture<FileDto> {
            check(newFileName != null || newFolderId != null) {
                "cannot file [fileId=$fileId]: either new file name or folder id must be specified"
            }

            val requestUrl = fileUri(fileId)
            val capture = Capture.empty<FileDto>()

            return http.put(requestUrl)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.USER_AGENT, config.userAgent)
                    .headers(oauthHeaders(
                            config = config,
                            httpMethod = HttpMethod.PUT,
                            requestPath = requestUrl
                    ))
                    .body(FileChangeDto(name = newFileName, folderId = newFolderId))
                    .dispatch(Navigators.series(),
                            Bindings.on(HttpStatus.Series.SUCCESSFUL).call(FileDto::class.java, capture),
                            Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                    )
                    .thenApply(capture);
        }


        fun deleteFile(fileId: UUID): CompletableFuture<Void> {
            val requestUrl = fileUri(fileId)
            val capture = Capture.empty<Void>()

            return http.delete(requestUrl)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.USER_AGENT, config.userAgent)
                    .headers(oauthHeaders(
                            config = config,
                            httpMethod = HttpMethod.DELETE,
                            requestPath = requestUrl
                    ))
                    .dispatch(Navigators.series(),
                            Bindings.on(HttpStatus.Series.SUCCESSFUL).call(Void::class.java, capture),
                            Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                    )
                    .thenApply(capture);
        }
    }

}
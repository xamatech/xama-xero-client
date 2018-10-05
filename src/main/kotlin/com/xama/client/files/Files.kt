package com.xama.client.files

import com.google.common.collect.HashMultimap
import com.xama.client.*
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

    enum class SortDirection {
        ASC,
        DESC
    }

    enum class SortField(val attributeName: String) {
        CREATED("CreatedDateUTC"),
        NAME("Name"),
        SIZE("Size")
    }

    private data class FileChangeDto(val name: String?, val folderId: UUID?)


    // Bindings.on(HttpStatus.Series.SUCCESSFUL).call({ response: ClientHttpResponse, reader: MessageReader -> println(response.body.bufferedReader().use { it.readText() }) }),


    class Client (val http: Http, val config: Config){
        companion object {
            const val BASE_URL = "https://api.xero.com/files.xro/1.0/files"
            const val DEFAULT_PAGE_SIZE = 100
            const val DEFAULT_PAGE = 1
            val DEFAULT_SORT = Pair(SortField.CREATED, SortDirection.DESC)
        }

        private fun fileUri(fileId: UUID) = "$BASE_URL/$fileId"

        fun getFiles(
                pageSize: Int = DEFAULT_PAGE_SIZE,
                page: Int = DEFAULT_PAGE,
                sortParameter: Pair<SortField, SortDirection> = DEFAULT_SORT
        ): CompletableFuture<GetFilesResponseDto> = getFiles(
                pageSize = pageSize,
                page = page,
                sortParameter = sortParameter,
                credentials = config.credentialsProvider!!.invoke()
        )


        fun getFiles(
                pageSize: Int = DEFAULT_PAGE_SIZE,
                page: Int = DEFAULT_PAGE,
                sortParameter: Pair<SortField, SortDirection> = DEFAULT_SORT,
                credentials: Credentials
        ): CompletableFuture<GetFilesResponseDto> {

            val queryParameterPairs = listOf(
                    Pair("pagesize", pageSize.toString()),
                    Pair("page", page.toString())
                    // Pair("sort", "${sortParameter.first.attributeName} ${sortParameter.second.name}") FIXME problem with signature, if specified
            )

            val queryParams = HashMultimap.create<String, String>()
            queryParameterPairs.forEach { queryParams.put(it.first, it.second) }


            val capture = Capture.empty<GetFilesResponseDto>()
            return http.get(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .queryParams(queryParams)
                    .header(HttpHeaders.USER_AGENT, config.userAgent)
                    .headers(oauthHeaders(
                            config = config,
                            credentials = credentials,
                            httpMethod = HttpMethod.GET,
                            requestPath = BASE_URL,
                            parameters = queryParameterPairs
                    ))
                    .dispatch(Navigators.series(),
                            Bindings.on(HttpStatus.Series.SUCCESSFUL).call(GetFilesResponseDto::class.java, capture),
                            Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                    )
                    .thenApply(capture)
        }


        fun getFile(fileId: UUID): CompletableFuture<FileDto> = getFile(
                fileId = fileId,
                credentials = config.credentialsProvider!!.invoke()
        )


        fun getFile(fileId: UUID, credentials: Credentials): CompletableFuture<FileDto> {
            val requestUrl = fileUri(fileId)
            val capture = Capture.empty<FileDto>()

            return http.get(requestUrl)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.USER_AGENT, config.userAgent)
                    .headers(oauthHeaders(
                            credentials = credentials,
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



        fun uploadFile(fileName: String,
                       fileUrl: URL): CompletableFuture<FileDto> = uploadFile(
                fileName = fileName,
                fileUrl = fileUrl,
                credentials = config.credentialsProvider!!.invoke()

        )


        fun uploadFile(fileName: String,
                       fileUrl: URL,
                       credentials: Credentials
        ): CompletableFuture<FileDto> = uploadFile(
                fileName = fileName,
                bytes = fileUrl.readBytes(),
                credentials = credentials
        )


        fun uploadFile(fileName: String, bytes: ByteArray): CompletableFuture<FileDto> = uploadFile(
                fileName = fileName,
                bytes = bytes,
                credentials = config.credentialsProvider!!.invoke()
        )


        fun uploadFile(fileName: String, bytes:
                       ByteArray,
                       credentials: Credentials
        ): CompletableFuture<FileDto> {
            val multiValueMap = LinkedMultiValueMap<String, Any>()
            multiValueMap[fileName] = ExtendedResource(fileName, bytes)

            val capture = Capture.empty<FileDto>()
            return http.post(BASE_URL)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.USER_AGENT, config.userAgent)
                    .headers(oauthHeaders(
                            config = config,
                            credentials = credentials,
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


        fun changeFile(fileId: UUID,
                       newFileName: String? = null,
                       newFolderId: UUID? = null
        ): CompletableFuture<FileDto> = changeFile(
                fileId = fileId,
                newFileName = newFileName,
                newFolderId = newFolderId,
                credentials = config.credentialsProvider!!.invoke()
        )


        fun changeFile(fileId: UUID,
                       newFileName: String? = null,
                       newFolderId: UUID? = null,
                       credentials: Credentials): CompletableFuture<FileDto> {

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
                            credentials = credentials,
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

        fun deleteFile(fileId: UUID): CompletableFuture<Void> = deleteFile(
                fileId = fileId,
                credentials = config.credentialsProvider!!.invoke()
        )

        fun deleteFile(fileId: UUID, credentials: Credentials): CompletableFuture<Void> {
            val requestUrl = fileUri(fileId)
            val capture = Capture.empty<Void>()

            return http.delete(requestUrl)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.USER_AGENT, config.userAgent)
                    .headers(oauthHeaders(
                            credentials = credentials,
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
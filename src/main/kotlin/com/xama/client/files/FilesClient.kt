package com.xama.client.files

import com.google.common.collect.HashMultimap
import com.xama.client.Credentials
import com.xama.client.ExtendedResource
import com.xama.client.handleProblem
import org.apache.tika.Tika
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
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




/**
 * see https://developer.xero.com/documentation/files-api/files
 */
class FilesClient(private val http: Http) {

    private val tika: Tika = Tika()

    companion object {
        const val BASE_URL = "https://api.xero.com/files.xro/1.0/files"
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
        val DEFAULT_SORT = Pair(SortField.CREATED, SortDirection.DESC)
    }


    private fun fileUri(fileId: UUID) = "$BASE_URL/$fileId"


    fun getFiles(
            credentials: Credentials,
            pageSize: Int = DEFAULT_PAGE_SIZE,
            page: Int = DEFAULT_PAGE,
            sortParameter: Pair<SortField, SortDirection> = DEFAULT_SORT
    ): CompletableFuture<GetFilesResponseDto> {

        val queryParameterPairs = listOf(
                Pair("pagesize", pageSize.toString()),
                Pair("page", page.toString()),
                Pair("sort", "${sortParameter.first.attributeName} ${sortParameter.second.name}")
        )

        val queryParams = HashMultimap.create<String, String>()
        queryParameterPairs.forEach { queryParams.put(it.first, it.second) }


        val capture = Capture.empty<GetFilesResponseDto>()
        return http.get(BASE_URL)
                .queryParams(queryParams)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(credentials.toHttpHeaders())
                .dispatch(Navigators.series(),
                        Bindings.on(HttpStatus.Series.SUCCESSFUL).call(GetFilesResponseDto::class.java, capture),
                        Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                )
                .thenApply(capture)
    }


    fun getFile(credentials: Credentials, fileId: UUID): CompletableFuture<FileDto> {
        val requestUrl = fileUri(fileId)
        val capture = Capture.empty<FileDto>()

        return http.get(requestUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(credentials.toHttpHeaders())
                .dispatch(Navigators.series(),
                        Bindings.on(HttpStatus.Series.SUCCESSFUL).call(FileDto::class.java, capture),
                        Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                )
                .thenApply(capture)
    }


    fun uploadFile(
            credentials: Credentials,
            fileName: String,
            fileUrl: URL
    ): CompletableFuture<FileDto> = uploadFile(
            credentials = credentials,
            fileName = fileName,
            bytes = fileUrl.readBytes()
    )


    fun uploadFile(credentials: Credentials, fileName: String, bytes: ByteArray): CompletableFuture<FileDto> {
        val resourceHeaders = HttpHeaders();
        resourceHeaders.contentType = MediaType.parseMediaType(tika.detect(fileName))

        val resource = ExtendedResource(fileName, bytes)
        val fileEntity = HttpEntity(resource, resourceHeaders)

        val multiValueMap = LinkedMultiValueMap<String, Any>()
        multiValueMap[fileName] = fileEntity

        val capture = Capture.empty<FileDto>()
        return http.post(BASE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .headers(credentials.toHttpHeaders())
                .body(multiValueMap)
                .dispatch(Navigators.series(),
                        Bindings.on(HttpStatus.Series.SUCCESSFUL).call(FileDto::class.java, capture),
                        Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                )
                .thenApply(capture)
    }


    fun uploadFile2(credentials: Credentials, fileName: String, bytes: ByteArray): CompletableFuture<Any> {
        val resourceHeaders = HttpHeaders();
        resourceHeaders.contentType = MediaType.parseMediaType(tika.detect(fileName))

        val resource = ExtendedResource(fileName, bytes)
        val fileEntity = HttpEntity(resource, resourceHeaders)

        val multiValueMap = LinkedMultiValueMap<String, Any>()
        multiValueMap[fileName] = fileEntity

        val capture = Capture.empty<Any>()
        return http.post(BASE_URL)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON)
            .headers(credentials.toHttpHeaders())
            .body(multiValueMap)
            .dispatch(Navigators.series(),
                Bindings.on(HttpStatus.Series.SUCCESSFUL).call(Any::class.java, capture),
                Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
            )
            .thenApply(capture)
    }


    fun changeFile(
            credentials: Credentials,
            fileId: UUID,
            newFileName: String? = null,
            newFolderId: UUID? = null
   ): CompletableFuture<FileDto> {
        check(newFileName != null || newFolderId != null) {
            "cannot file [fileId=$fileId]: either new file name or folder id must be specified"
        }

        val requestUrl = fileUri(fileId)
        val capture = Capture.empty<FileDto>()

        return http.put(requestUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(credentials.toHttpHeaders())
                .body(FileChangeDto(name = newFileName, folderId = newFolderId))
                .dispatch(Navigators.series(),
                        Bindings.on(HttpStatus.Series.SUCCESSFUL).call(FileDto::class.java, capture),
                        Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                )
                .thenApply(capture)
    }


    fun deleteFile(credentials: Credentials, fileId: UUID): CompletableFuture<Void> {
        val requestUrl = fileUri(fileId)
        val capture = Capture.empty<Void>()

        return http.delete(requestUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(credentials.toHttpHeaders())
                .dispatch(Navigators.series(),
                        Bindings.on(HttpStatus.Series.SUCCESSFUL).call(Void::class.java, capture),
                        Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                )
                .thenApply(capture)
    }
}


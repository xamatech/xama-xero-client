package com.xama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xama.client.ConfigUtilsKt;
import com.xama.client.files.*;
import kotlin.Pair;
import org.apache.http.HttpRequestFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.RawRequestFilter;
import org.zalando.logbook.StreamHttpLogWriter;
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor;
import org.zalando.logbook.httpclient.LogbookHttpResponseInterceptor;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.OriginalStackTracePlugin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.xama.TestUtils.toByteArray;
import static org.junit.Assert.*;

public class FilesClientIT {

    private FilesClient client;

    @Before
    public void setup() {
        final Http http = ConfigUtilsKt.configureHttp();
        
        client = new FilesClient(http);
    }

    @Test
    public void testUpload() throws Exception {
        final CompletableFuture<FileDto> future = client.uploadFile(
                TestUtils.CREDENTIALS,
                "test.jpg",
                toByteArray("/test.jpg")
        );
        final FileDto fileDto = future.join();

        assertEquals("test.jpg", fileDto.getName());
        assertEquals("image/jpeg", fileDto.getMimeType());
        assertNotNull("folderId must not be null", fileDto.getFolderId());
        assertNotNull("fileId must not be null", fileDto.getId());
        assertTrue("invalid file size", fileDto.getSize() > 0);
        assertNotNull("CreatedDateUtc must not be null", fileDto.getCreatedDateUtc());
        assertNotNull("UpdatedDateUtc must not be null", fileDto.getUpdatedDateUtc());

        final UserDto user = fileDto.getUser();
        assertNotNull("user must not be null", user);
        assertNotNull("user id must not be null", user.getId());
        assertNotNull("firstName must not be null", user.getFirstName());
        assertNotNull("lastName must not be null", user.getLastName());
        assertNotNull("name must not be null", user.getName());
        assertNotNull("fullName must not be null", user.getFullName());
    }


    @Test
    public void testUploadDifferentFileTypes() throws Exception {
        CompletableFuture<FileDto> future = client.uploadFile(
                TestUtils.CREDENTIALS,
                "test_files_client.jpg",
                toByteArray("/test.jpg")
        );
        FileDto fileDto = future.join();
        assertEquals("image/jpeg", fileDto.getMimeType());

        future = client.uploadFile(
                TestUtils.CREDENTIALS,
                "test_files_client.pdf",
                toByteArray("/test.pdf")
        );
        fileDto = future.join();
        assertEquals("application/pdf", fileDto.getMimeType());

        future = client.uploadFile(
                TestUtils.CREDENTIALS,
                "test_files_client.xlsx",
                toByteArray("/test.xlsx")
        );
        fileDto = future.join();
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fileDto.getMimeType());
    }


    @Test
    public void testGetFiles() {
        final CompletableFuture<GetFilesResponseDto> future = client.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        );

        final GetFilesResponseDto response = future.join();

        assertEquals("wrong page", FilesClient.DEFAULT_PAGE, response.getPage());
        assertEquals("wrong page size", FilesClient.DEFAULT_PAGE_SIZE, response.getPerPage());

        final List<FileDto> items = response.getItems();
        assertNotNull("items list must not be null", items);
        assertFalse("list of files must not be empty", items.isEmpty());
    }


    @Test
    public void testGetFile() {
        final GetFilesResponseDto filesResponse = client.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ).join();

        final List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final FileDto retrievedFile = client.getFile(TestUtils.CREDENTIALS, sourceFile.getId()).join();
        assertEquals("wrong file retrieved", sourceFile, retrievedFile);
    }


    @Test
    public void testDeleteFile() {
        GetFilesResponseDto filesResponse = client.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ).join();

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        client.deleteFile(TestUtils.CREDENTIALS, sourceFile.getId()).join();

        filesResponse = client.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ).join();

        final Optional<FileDto> found = filesResponse.getItems()
                .stream()
                .filter(f -> Objects.equals(f.getId(), sourceFile.getId()))
                .findFirst();

        assertFalse("file should not be available anymore", found.isPresent());
    }
}

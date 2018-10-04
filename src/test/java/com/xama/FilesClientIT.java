package com.xama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xama.client.ConfigUtilsKt;
import com.xama.client.files.Files;
import kotlin.Pair;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.RawRequestFilter;
import org.zalando.logbook.StreamHttpLogWriter;
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor;
import org.zalando.riptide.Http;
import org.zalando.riptide.capture.Completion;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class FilesClientIT {

    private Files.Client client;

    @Before
    public void setup() {
        final Logbook logbook = Logbook.builder()
                .clearBodyFilters()
                .clearHeaderFilters()
                .clearRawRequestFilters()
                .rawRequestFilter(RawRequestFilter.none())
                .clearRawResponseFilters()
                .clearRequestFilters()
                .writer(new StreamHttpLogWriter(System.err))
                .build();

        final HttpClient httpClient = HttpClientBuilder.create()
                // TODO configure client here
                .addInterceptorFirst(new LogbookHttpRequestInterceptor(logbook))
                .build();

        final ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();

        //------

        final ObjectMapper objectMapper = ConfigUtilsKt.configureObjectMapper(new Jackson2ObjectMapperBuilder()).build();

        final Http http = ConfigUtilsKt.configureHttp(
                Http.builder().requestFactory(
                        new RestAsyncClientHttpRequestFactory(httpClient, executor)),
                        objectMapper
        )
        .build();


        client = new Files.Client(http, TestConfig.INSTANCE.getTestConfig());

    }

    @org.junit.Test
    public void testUpload() throws Exception {
        final CompletableFuture<Files.FileDto> future = client.uploadFile("test.jpg", TestConfig.INSTANCE.getTestImageUrl());
        final Files.FileDto fileDto = Completion.join(future);

        assertEquals("test.jpg", fileDto.getName());
        assertEquals("image/jpeg", fileDto.getMimeType());
        assertNotNull("folderId must not be null", fileDto.getFolderId());
        assertNotNull("fileId must not be null", fileDto.getId());
        assertTrue("invalid file size", fileDto.getSize() > 0);
        assertNotNull("CreatedDateUtc must not be null", fileDto.getCreatedDateUtc());
        assertNotNull("UpdatedDateUtc must not be null", fileDto.getUpdatedDateUtc());

        final Files.UserDto user = fileDto.getUser();
        assertNotNull("user must not be null", user);
        assertNotNull("user id must not be null", user.getId());
        assertNotNull("firstName must not be null", user.getFirstName());
        assertNotNull("lastName must not be null", user.getLastName());
        assertNotNull("name must not be null", user.getName());
        assertNotNull("fullName must not be null", user.getFullName());
    }


    @org.junit.Test
    public void testGetFiles() {
        final CompletableFuture<Files.GetFilesResponseDto> future = client.getFiles(
                Files.Client.DEFAULT_PAGE_SIZE,
                Files.Client.DEFAULT_PAGE,
                new Pair<>(Files.SortField.CREATED, Files.SortDirection.DESC)
        );

        final Files.GetFilesResponseDto response = Completion.join(future);

        assertEquals("wrong page", Files.Client.DEFAULT_PAGE, response.getPage());
        assertEquals("wrong page size", Files.Client.DEFAULT_PAGE_SIZE, response.getPerPage());

        final List<Files.FileDto> items = response.getItems();
        assertNotNull("items list must not be null", items);
        assertFalse("list of files must not be null", items.isEmpty());
    }


    @org.junit.Test
    public void testGetFile() {
        final Files.GetFilesResponseDto filesResponse = Completion.join(client.getFiles(
                Files.Client.DEFAULT_PAGE_SIZE,
                Files.Client.DEFAULT_PAGE,
                new Pair<>(Files.SortField.CREATED, Files.SortDirection.DESC)
        ));

        final List<Files.FileDto> files = filesResponse.getItems();
        final Files.FileDto sourceFile = files.get(0);

        final Files.FileDto retrievedFile = Completion.join(client.getFile(sourceFile.getId()));
        assertEquals("wrong file retrieved", sourceFile, retrievedFile);
    }


    @org.junit.Test
    public void testDeleteFile() {
        Files.GetFilesResponseDto filesResponse = Completion.join(client.getFiles(
                Files.Client.DEFAULT_PAGE_SIZE,
                Files.Client.DEFAULT_PAGE,
                new Pair<>(Files.SortField.CREATED, Files.SortDirection.DESC)
        ));

        List<Files.FileDto> files = filesResponse.getItems();
        final Files.FileDto sourceFile = files.get(0);

        Completion.join(client.deleteFile(sourceFile.getId()));

        filesResponse = Completion.join(client.getFiles(
                Files.Client.DEFAULT_PAGE_SIZE,
                Files.Client.DEFAULT_PAGE,
                new Pair<>(Files.SortField.CREATED, Files.SortDirection.DESC)
        ));

        final Optional<Files.FileDto> found = filesResponse.getItems()
                .stream()
                .filter(f -> Objects.equals(f.getId(), sourceFile.getId()))
                .findFirst();

        assertFalse("file should not be available anymore", found.isPresent());
    }
}

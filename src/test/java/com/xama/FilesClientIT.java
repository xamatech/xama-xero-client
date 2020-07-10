package com.xama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xama.client.ConfigUtilsKt;
import com.xama.client.Credentials;
import com.xama.client.files.*;
import kotlin.Pair;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.xama.TestUtils.toByteArray;
import static org.junit.Assert.*;

public class FilesClientIT {

    private FilesClient client;
    private Credentials credentials;

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
        
        client = new FilesClient(http);

        credentials = new Credentials(
                "accessToken",
                UUID.randomUUID(),
                "xama-xero-client-test"
        );

    }

    @Test
    public void testUpload() throws Exception {
        final CompletableFuture<FileDto> future = client.uploadFile(
                credentials,
                "test.jpg",
                toByteArray("/test.jpg")
        );
        final FileDto fileDto = Completion.join(future);

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
                credentials,
                "test_files_client.jpg",
                toByteArray("/test.jpg")
        );
        FileDto fileDto = Completion.join(future);
        assertEquals("image/jpeg", fileDto.getMimeType());

        future = client.uploadFile(
                credentials,
                "test_files_client.pdf",
                toByteArray("/test.pdf")
        );
        fileDto = Completion.join(future);
        assertEquals("application/pdf", fileDto.getMimeType());

        future = client.uploadFile(
                credentials,
                "test_files_client.xlsx",
                toByteArray("/test.xlsx")
        );
        fileDto = Completion.join(future);
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fileDto.getMimeType());
    }


    @Test
    public void testGetFiles() {
        final CompletableFuture<GetFilesResponseDto> future = client.getFiles(
                credentials,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        );

        final GetFilesResponseDto response = Completion.join(future);

        assertEquals("wrong page", FilesClient.DEFAULT_PAGE, response.getPage());
        assertEquals("wrong page size", FilesClient.DEFAULT_PAGE_SIZE, response.getPerPage());

        final List<FileDto> items = response.getItems();
        assertNotNull("items list must not be null", items);
        assertFalse("list of files must not be null", items.isEmpty());
    }


    @Test
    public void testGetFile() {
        final GetFilesResponseDto filesResponse = Completion.join(client.getFiles(
                credentials,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ));

        final List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final FileDto retrievedFile = Completion.join(client.getFile(credentials, sourceFile.getId()));
        assertEquals("wrong file retrieved", sourceFile, retrievedFile);
    }


    @Test
    public void testDeleteFile() {
        GetFilesResponseDto filesResponse = Completion.join(client.getFiles(
                credentials,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ));

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        Completion.join(client.deleteFile(credentials, sourceFile.getId()));

        filesResponse = Completion.join(client.getFiles(
                credentials,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ));

        final Optional<FileDto> found = filesResponse.getItems()
                .stream()
                .filter(f -> Objects.equals(f.getId(), sourceFile.getId()))
                .findFirst();

        assertFalse("file should not be available anymore", found.isPresent());
    }
}

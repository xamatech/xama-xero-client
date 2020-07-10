package com.xama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xama.client.ConfigUtilsKt;
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
import java.util.UUID;

import static org.junit.Assert.*;

public class AssocationsClientIT {

    private FilesClient filesClient;
    private AssociationsClient client;

    private static final UUID BANK_TRANSACTION_ID = UUID.fromString("cc9b46e5-1dc7-46a2-a1fa-ed387c15437f");


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


        client = new AssociationsClient(http);
        filesClient = new FilesClient(http);
    }


    @org.junit.Test
    public void testCreateAssociation() {
        final GetFilesResponseDto filesResponse = Completion.join(filesClient.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ));

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final AssociationDto association = Completion.join(client.createAssociation(
                TestUtils.CREDENTIALS,
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ));

        assertEquals("wrong file id", sourceFile.getId(), association.getFileId());
        assertEquals("wrong object id", BANK_TRANSACTION_ID, association.getObjectId());
        assertEquals("wrong object group", ObjectGroup.BANKTRANSACTION, association.getObjectGroup());
        assertEquals("wrong object type", ObjectType.CASHREC, association.getObjectType());
    }


    @Test
    public void testGetFileAssociations() {
        final GetFilesResponseDto filesResponse = Completion.join(filesClient.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ));

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final AssociationDto association = Completion.join(client.createAssociation(
                TestUtils.CREDENTIALS,
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ));


        final List<AssociationDto> associations = Completion.join(
                client.getFileAssociations(TestUtils.CREDENTIALS, sourceFile.getId())
        );
        assertNotNull("list of retrieved associations must not be null", associations);
        assertEquals("expected 1 association", 1, associations.size());
        assertEquals("wrong association", association, associations.get(0));
    }


    @Test
    public void testGetObjectAssociations() {
        final GetFilesResponseDto filesResponse = Completion.join(filesClient.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ));

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final AssociationDto association = Completion.join(client.createAssociation(
                TestUtils.CREDENTIALS,
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ));


        final List<AssociationDto> associations = Completion.join(
                client.getObjectAssociations(TestUtils.CREDENTIALS, BANK_TRANSACTION_ID)
        );
        assertNotNull("list of retrieved associations must not be null", associations);
        assertEquals("expected 1 association", 1, associations.size());
        assertEquals("wrong association", association, associations.get(0));
    }


    @Test
    public void testDeleteAssociation() throws Exception {
        final FileDto file = Completion.join(filesClient.uploadFile(
                TestUtils.CREDENTIALS,
                "testDeleteAssociation.jpg",
                TestUtils.toByteArray("/test.jpg")
        ));

        Completion.join(client.createAssociation(
                TestUtils.CREDENTIALS,
                file.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ));

        List<AssociationDto> associations = Completion.join(
                client.getFileAssociations(TestUtils.CREDENTIALS, file.getId())
        );

        assertTrue(
                "association was not created",
                associations.stream().anyMatch(f -> Objects.equals(f.getFileId(), file.getId()))
        );

        Completion.join(client.deleteAssociation(TestUtils.CREDENTIALS, file.getId(), BANK_TRANSACTION_ID));

        associations = Completion.join(
                client.getFileAssociations(TestUtils.CREDENTIALS, file.getId())
        );

        assertFalse(
                "association was not deleted",
                associations.stream().anyMatch(f -> Objects.equals(f.getFileId(), file.getId()))
        );
    }

}

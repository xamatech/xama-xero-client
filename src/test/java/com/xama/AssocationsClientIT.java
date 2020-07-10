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
import java.util.UUID;

import static org.junit.Assert.*;

public class AssocationsClientIT {

    private FilesClient filesClient;
    private AssociationsClient client;
    private Credentials credentials;

    private static final UUID BANK_TRANSACTION_ID = UUID.fromString("e654a6a0-5492-4875-9964-e21bd46b8b4f");


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

        credentials = new Credentials(
                "accessToken",
                UUID.randomUUID(),
                "xama-xero-client-test"
        );
    }


    @org.junit.Test
    public void testCreateAssociation() {
        final GetFilesResponseDto filesResponse = Completion.join(filesClient.getFiles(
                credentials,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ));

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final AssociationDto association = Completion.join(client.createAssociation(
                credentials,
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
                credentials,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ));

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final AssociationDto association = Completion.join(client.createAssociation(
                credentials,
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ));


        final List<AssociationDto> associations = Completion.join(
                client.getFileAssociations(credentials, sourceFile.getId())
        );
        assertNotNull("list of retrieved associations must not be null", associations);
        assertEquals("expected 1 association", 1, associations.size());
        assertEquals("wrong association", association, associations.get(0));
    }


    @Test
    public void testGetObjectAssociations() {
        final GetFilesResponseDto filesResponse = Completion.join(filesClient.getFiles(
                credentials,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ));

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final AssociationDto association = Completion.join(client.createAssociation(
                credentials,
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ));


        final List<AssociationDto> associations = Completion.join(
                client.getObjectAssociations(credentials, BANK_TRANSACTION_ID)
        );
        assertNotNull("list of retrieved associations must not be null", associations);
        assertEquals("expected 1 association", 1, associations.size());
        assertEquals("wrong association", association, associations.get(0));
    }


    @Test
    public void testDeleteAssociation() throws Exception {
        final FileDto file = Completion.join(filesClient.uploadFile(
                credentials,
                "testDeleteAssociation.jpg",
                TestUtils.toByteArray("/test.jpg")
        ));

        Completion.join(client.createAssociation(
                credentials,
                file.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ));

        List<AssociationDto> associations = Completion.join(
                client.getFileAssociations(credentials, file.getId())
        );

        assertTrue(
                "association was not created",
                associations.stream().anyMatch(f -> Objects.equals(f.getFileId(), file.getId()))
        );

        Completion.join(client.deleteAssociation(credentials, file.getId(), BANK_TRANSACTION_ID));

        associations = Completion.join(
                client.getFileAssociations(credentials, file.getId())
        );

        assertFalse(
                "association was not deleted",
                associations.stream().anyMatch(f -> Objects.equals(f.getFileId(), file.getId()))
        );
    }

}

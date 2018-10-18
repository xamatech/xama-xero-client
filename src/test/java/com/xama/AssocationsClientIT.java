package com.xama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xama.client.ConfigUtilsKt;
import com.xama.client.files.Associations;
import com.xama.client.files.Files;
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

    private Files.Client filesClient;
    private Associations.Client client;

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


        client = new Associations.Client(http, TestConfig.INSTANCE.getTestConfig());
        filesClient = new Files.Client(http, TestConfig.INSTANCE.getTestConfig());

    }


    @org.junit.Test
    public void testCreateAssociation() {
        final Files.GetFilesResponseDto filesResponse = Completion.join(filesClient.getFiles(
                Files.Client.DEFAULT_PAGE_SIZE,
                Files.Client.DEFAULT_PAGE,
                new Pair<>(Files.SortField.CREATED, Files.SortDirection.DESC)
        ));

        List<Files.FileDto> files = filesResponse.getItems();
        final Files.FileDto sourceFile = files.get(0);

        final Associations.AssociationDto association = Completion.join(client.createAssociation(
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                Associations.ObjectGroup.BANKTRANSACTION
        ));

        assertEquals("wrong file id", sourceFile.getId(), association.getFileId());
        assertEquals("wrong object id", BANK_TRANSACTION_ID, association.getObjectId());
        assertEquals("wrong object group", Associations.ObjectGroup.BANKTRANSACTION, association.getObjectGroup());
        assertEquals("wrong object type", Associations.ObjectType.CASHREC, association.getObjectType());
    }


    @Test
    public void testGetFileAssociations() {
        final Files.GetFilesResponseDto filesResponse = Completion.join(filesClient.getFiles(
                Files.Client.DEFAULT_PAGE_SIZE,
                Files.Client.DEFAULT_PAGE,
                new Pair<>(Files.SortField.CREATED, Files.SortDirection.DESC)
        ));

        List<Files.FileDto> files = filesResponse.getItems();
        final Files.FileDto sourceFile = files.get(0);

        final Associations.AssociationDto association = Completion.join(client.createAssociation(
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                Associations.ObjectGroup.BANKTRANSACTION
        ));


        final List<Associations.AssociationDto> associations = Completion.join(
                client.getFileAssociations(sourceFile.getId())
        );
        assertNotNull("list of retrieved associations must not be null", associations);
        assertEquals("expected 1 association", 1, associations.size());
        assertEquals("wrong association", association, associations.get(0));
    }


    @Test
    public void testGetObjectAssociations() {
        final Files.GetFilesResponseDto filesResponse = Completion.join(filesClient.getFiles(
                Files.Client.DEFAULT_PAGE_SIZE,
                Files.Client.DEFAULT_PAGE,
                new Pair<>(Files.SortField.CREATED, Files.SortDirection.DESC)
        ));

        List<Files.FileDto> files = filesResponse.getItems();
        final Files.FileDto sourceFile = files.get(0);

        final Associations.AssociationDto association = Completion.join(client.createAssociation(
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                Associations.ObjectGroup.BANKTRANSACTION
        ));


        final List<Associations.AssociationDto> associations = Completion.join(
                client.getObjectAssociations(BANK_TRANSACTION_ID)
        );
        assertNotNull("list of retrieved associations must not be null", associations);
        assertEquals("expected 1 association", 1, associations.size());
        assertEquals("wrong association", association, associations.get(0));
    }


    @Test
    public void testDeleteAssociation() throws Exception {
        final Files.FileDto file = Completion.join(filesClient.uploadFile(
                "testDeleteAssociation.jpg",
                TestUtils.toByteArray("/test.jpg")
        ));

        final Associations.AssociationDto association = Completion.join(client.createAssociation(
                file.getId(),
                BANK_TRANSACTION_ID,
                Associations.ObjectGroup.BANKTRANSACTION
        ));

        List<Associations.AssociationDto> associations = Completion.join(
                client.getFileAssociations(file.getId())
        );

        assertTrue(
                "association was not created",
                associations.stream().filter(f -> Objects.equals(f.getFileId(), file.getId())).findFirst().isPresent()
        );

        Completion.join(client.deleteAssociation(file.getId(), BANK_TRANSACTION_ID));

        associations = Completion.join(
                client.getFileAssociations(file.getId())
        );

        assertFalse(
                "association was not deleted",
                associations.stream().filter(f -> Objects.equals(f.getFileId(), file.getId())).findFirst().isPresent()
        );
    }

}

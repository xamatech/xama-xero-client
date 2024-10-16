package com.xama;

import com.xama.client.ConfigUtilsKt;
import com.xama.client.files.*;
import kotlin.Pair;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.RawRequestFilter;
import org.zalando.logbook.StreamHttpLogWriter;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;

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
        final Http http = ConfigUtilsKt.configureHttp();

        client = new AssociationsClient(http);
        filesClient = new FilesClient(http);
    }


    @org.junit.Test
    public void testCreateAssociation() {
        final GetFilesResponseDto filesResponse = filesClient.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ).join();

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final AssociationDto association = client.createAssociation(
                TestUtils.CREDENTIALS,
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ).join();

        assertEquals("wrong file id", sourceFile.getId(), association.getFileId());
        assertEquals("wrong object id", BANK_TRANSACTION_ID, association.getObjectId());
        assertEquals("wrong object group", ObjectGroup.BANKTRANSACTION, association.getObjectGroup());
        assertEquals("wrong object type", ObjectType.CASHREC, association.getObjectType());
    }


    @Test
    public void testGetFileAssociations() {
        final GetFilesResponseDto filesResponse = filesClient.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ).join();

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final AssociationDto association = client.createAssociation(
                TestUtils.CREDENTIALS,
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ).join();


        final List<AssociationDto> associations = client.getFileAssociations(
                TestUtils.CREDENTIALS,
                sourceFile.getId()
        ).join();

        assertNotNull("list of retrieved associations must not be null", associations);
        assertEquals("expected 1 association", 1, associations.size());
        assertEquals("wrong association", association, associations.get(0));
    }


    @Test
    public void testGetObjectAssociations() {
        final GetFilesResponseDto filesResponse = filesClient.getFiles(
                TestUtils.CREDENTIALS,
                FilesClient.DEFAULT_PAGE_SIZE,
                FilesClient.DEFAULT_PAGE,
                new Pair<>(SortField.CREATED, SortDirection.DESC)
        ).join();

        List<FileDto> files = filesResponse.getItems();
        final FileDto sourceFile = files.get(0);

        final AssociationDto association = client.createAssociation(
                TestUtils.CREDENTIALS,
                sourceFile.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ).join();


        final List<AssociationDto> associations = client.getObjectAssociations(
                TestUtils.CREDENTIALS,
                BANK_TRANSACTION_ID
        ).join();
        assertNotNull("list of retrieved associations must not be null", associations);
        assertEquals("expected 1 association", 1, associations.size());
        assertEquals("wrong association", association, associations.get(0));
    }


    @Test
    public void testDeleteAssociation() throws Exception {
        final FileDto file = filesClient.uploadFile(
                TestUtils.CREDENTIALS,
                "testDeleteAssociation.jpg",
                TestUtils.toByteArray("/test.jpg")
        ).join();

        client.createAssociation(
                TestUtils.CREDENTIALS,
                file.getId(),
                BANK_TRANSACTION_ID,
                ObjectGroup.BANKTRANSACTION
        ).join();

        List<AssociationDto> associations = client.getFileAssociations(TestUtils.CREDENTIALS, file.getId()).join();

        assertTrue(
                "association was not created",
                associations.stream().anyMatch(f -> Objects.equals(f.getFileId(), file.getId()))
        );

        client.deleteAssociation(TestUtils.CREDENTIALS, file.getId(), BANK_TRANSACTION_ID).join();

        associations = client.getFileAssociations(TestUtils.CREDENTIALS, file.getId()).join();

        assertFalse(
                "association was not deleted",
                associations.stream().anyMatch(f -> Objects.equals(f.getFileId(), file.getId()))
        );
    }

}

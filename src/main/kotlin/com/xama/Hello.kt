package com.xama

import com.fasterxml.jackson.databind.ObjectMapper
import com.xama.client.*
import com.xama.client.files.Files
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
import org.zalando.logbook.Logbook
import org.zalando.logbook.RawRequestFilter
import org.zalando.logbook.StreamHttpLogWriter
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor
import org.zalando.riptide.Http
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {

    //--- just needed for testing purposes
    val logbook = Logbook.builder()
            .clearBodyFilters()
            .clearHeaderFilters()
            .clearRawRequestFilters()
            .rawRequestFilter(RawRequestFilter.none())
            .clearRawResponseFilters()
            .clearRequestFilters()
            .writer(StreamHttpLogWriter(System.err))
            .build()

    val httpClient = HttpClientBuilder.create()
            // TODO configure client here
            .addInterceptorFirst(LogbookHttpRequestInterceptor(logbook))
            .build()

    val executor = ConcurrentTaskExecutor()

    //------

    val objectMapper: ObjectMapper = configureObjectMapper(Jackson2ObjectMapperBuilder()).build()

    val http = configureHttp(
            Http.builder().requestFactory(RestAsyncClientHttpRequestFactory(httpClient, executor)),
            objectMapper
    )
    .build()


    val config = Config(
            appType = AppType.PUBLIC,
            consumerKey = "...",
            consumerSecret = "...",
            credentialsProvider = {
                Credentials(
                        token="...",
                        tokenSecret="..."
                )
            }
    )


    val client = Files.Client(http = http, config = config)
    /*



    val before1 = System.currentTimeMillis()
    var future1 = client.getFiles()
    var result1 = future1.get(10L, TimeUnit.SECONDS);
    println(System.currentTimeMillis() - before1)
    println(result1)

    println("--------")

    val future = client.uploadFile("test.jpg", URL("file:///Users/bfriedrich/Desktop/test.jpeg"))
    val result = future.get(60L, TimeUnit.SECONDS);
    println(result)



    val future5 = client.getFile(
            fileId = UUID.fromString("967bc481-5ab1-4ec0-b37d-a0c103cdc7b4")
    )
    val result5 = future5.get(60L, TimeUnit.SECONDS);
    println(result5)





    val future = client.uploadFile("test.jpg", URL("file:///Users/bfriedrich/Desktop/test.jpeg"))
    val result = future.get(60L, TimeUnit.SECONDS);
    println(result)
  */


    var future1 = client.getFiles(page = 2, pageSize = 2)
    var result1 = future1.get(10L, TimeUnit.SECONDS);
    println(result1)


    /*
    val future6 = client.changeFile(
            fileId = result.id,
            newFileName = "another_file_name.jpeg"
    )
    val result6 = future6.get(60L, TimeUnit.SECONDS);
    println(result6)
*/





/*
val client = Associations.Client(http = http, config = config)


    val future = client.createAssociation(
            fileId = UUID.fromString("967bc481-5ab1-4ec0-b37d-a0c103cdc7b4"),
            objectId = UUID.fromString("e654a6a0-5492-4875-9964-e21bd46b8b4f"),
            objectGroup = Associations.ObjectGroup.BANKTRANSACTION
    )
    val result = future!!.get(60L, TimeUnit.SECONDS);
    println(result)

    println("--------")

    val future2 = client.getFileAssociations(UUID.fromString("967bc481-5ab1-4ec0-b37d-a0c103cdc7b4"))
    val result2 = future2.get(60L, TimeUnit.SECONDS)
    println(result2)


    println("--------")

    val future3 = client.getObjectAssociations(UUID.fromString("e654a6a0-5492-4875-9964-e21bd46b8b4f"))
    val result3 = future3.get(60L, TimeUnit.SECONDS)
    println(result3)

    println("--------")


    // filleId=967bc481-5ab1-4ec0-b37d-a0c103cdc7b4, objectId=e654a6a0-5492-4875-9964-e21bd46b8b4f
    val future4 = client.deleteAssociation(
            fileId = UUID.fromString("967bc481-5ab1-4ec0-b37d-a0c103cdc7b4"),
            objectId = UUID.fromString("e654a6a0-5492-4875-9964-e21bd46b8b4f")
    )
    val result4 = future4.get(60L, TimeUnit.SECONDS);
    println(result4)
      */




}


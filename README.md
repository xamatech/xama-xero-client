[![](https://jitpack.io/v/xamatech/xama-xero-client.svg)](https://jitpack.io/#xamatech/xama-xero-client)

# Xama Xero Client

**IMPORTANT: development is still in progress**

A Kotlin based Client implementation for accessing the
[Xero File API](https://developer.xero.com/documentation/files-api/overview-files) which
is heavily based on the [Zalando Riptide](https://github.com/zalando/riptide) HTTP client
and inspired by [XeroJava](https://github.com/XeroAPI/Xero-Java).

Please note that the implementation has not been fully completed but all
[Files](https://developer.xero.com/documentation/files-api/files) and
[Associations](https://developer.xero.com/documentation/files-api/associations)
endpoints can be accessed with the client.


## Prerequisites
- JVM >= 1.8
- Kotlin >= 1.2.61


## Installation

```xml
<dependencies>
    <dependency>
        <groupId>com.github.xamatech</groupId>
        <artifactId>xama-xero-client</artifactId>
        <version>{xama-xero-client-version}</version>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

## Configuration

### HTTP Client

The `Associations` and the `Files` related client requires a [Zalando Riptide](https://github.com/zalando/riptide) HTTP
client instance. Please consider the [Zalando Riptide documentation](https://github.com/zalando/riptide#configuration)
for its configuration. The `Xama Xero Client` provides utility methods to apply the required settings:

```java
final ObjectMapper objectMapper = ConfigUtilsKt.configureObjectMapper(new Jackson2ObjectMapperBuilder()).build();

final Http http = ConfigUtilsKt.configureHttp(
        Http.builder().requestFactory(
                new RestAsyncClientHttpRequestFactory(httpClient, executor)),
                objectMapper
)
.build();

```


## Usage

```java
final ObjectMapper objectMapper = ConfigUtilsKt.configureObjectMapper(new Jackson2ObjectMapperBuilder()).build();

final Http http = ConfigUtilsKt.configureHttp(
        Http.builder().requestFactory(
                new RestAsyncClientHttpRequestFactory(httpClient, executor)),
                objectMapper
)
.build();


final FilesClient client = new FilesClient(http);

final Credentials credentials = new Credentials("<accessToken>", tenantId, "myUserAgent");

final CompletableFuture<FileDto> future = client.uploadFile(credentials, "test.jpg", "file:///tmp/test.jpeg");
final FileDto file = Completion.join(future);


final AssociationsClient associationsClient = new AssociationsClient(http);
Completion.join(associationsClient.createAssociation(
    credentials,
    file.getId(),
    UUID.fromString("193FDBDA-4738-4AEA-8382-DFAF32F819B0"), // Xero Bank Transaction Id
    Associations.ObjectGroup.BANKTRANSACTION
));
```

Note: you can find examples for each functionality in our [tests](https://github.com/xamatech/xama-xero-client/tree/master/src/test/java/com/xama).


## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* [XeroJava](https://github.com/XeroAPI/Xero-Java)
* [Zalando Riptide](https://github.com/zalando/riptide)

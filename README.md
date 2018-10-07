# Xama Xero Client

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
        <groupId>org.zalando</groupId>
        <artifactId>riptide-core</artifactId>
        <version>${riptide.version}</version>
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

The `Associations` and the `Files` related client requires a `Config`
as well as a [Zalando Riptide](https://github.com/zalando/riptide) HTTP
client instance. Please consider the
[Zalando Riptide documentation](https://github.com/zalando/riptide#configuration)
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


### Public App

**Kotlin**:
```kotlin
Config.getPublicAppConfig(
    consumerKey = "<ConsumerKey>",
    consumerSecret = "<ConsumerSecret>"
)
```

**Java**:
```java
Config.Companion.getPublicAppConfig(
    "<ConsumerKey>",
    "<ConsumerSecret>",
    "My-Fancy-User-Agent"
);
```


### Private App

**Kotlin**:
```kotlin
Config.getPrivateAppConfig(
        consumerKey = "F1PPLNXCD12QOVWOKOZLON09UWJLMV",
        consumerSecret = "6UCRXSEVZLUOU5KPBTQHKSHPQDXVY5",
        privateKeyCert = "/public_privatekey.pfx"
        privateKeyPassword: String = ""
)
```

**Java**:
```java
Config.Companion.getPrivateAppConfig(
    "<ConsumerKey>",
    "<ConsumerSecret>",
    "<PathToPrivateKey>",
    "<PrivateKeyPassword>",
    "My-Fancy-User-Agent"
);
```


### Partner App

**Kotlin**:
```kotlin
Config.Companion.getPartnerAppConfig(
    "<ConsumerKey>",
    "<ConsumerSecret>",
    "<PathToPrivateKey>",
    "<PrivateKeyPassword>",
    "My-Fancy-User-Agent"
);
```

**Java**:
```java
Config.Companion.getPrivateAppConfig(
    "<ConsumerKey>",
    "<ConsumerSecret>",
    "<PathToPrivateKey>",
    "<PrivateKeyPassword>",
    "My-Fancy-User-Agent"
);
```

### Credentials Provider

You can specify Lambda object as Credentials Provider in the `Config` in order to fetch the
OAuth credentials when a client method is executed. This might be useful
for fetching possibly refreshed tokens e.g. from a DB. Please note that
a `Credentials Provider` is not mandatory. However, if it is not specified,
a `Credentials` object must be passed in each client method call.


#### WITH Credentials Provider

**Kotlin**:
```kotlin
    Config.getPublicAppConfig(
            consumerKey = "<ConsumerKey>",
            consumerSecret = "<ConsumerSecret>",
            credentialsProvider = {
                Credentials(
                        token="<token>",
                        tokenSecret="<tokenSecret>"
                )
            }
    )
```

**Java**:
```java
Config.Companion.getPublicAppConfig(
    "<ConsumerKey>",
    "<ConsumerSecret>",
    "My-Fancy-User-Agent",
    () -> new Credentials("<token>", "<tokenSecret>")
);
```

**Client:**
```java
client.uploadFile("test.jpg", "file:///tmp/test.jpeg");
```


#### WITHOUT Credentials Provider

**Kotlin**:
```kotlin
    Config.getPublicAppConfig(
            consumerKey = "<ConsumerKey>",
            consumerSecret = "<ConsumerSecret>"
    )
```

**Java**:
```java
Config.Companion.getPublicAppConfig(
    "<ConsumerKey>",
    "<ConsumerSecret>",
    "My-Fancy-User-Agent"
);
```

**Client:**

*Kotlin:*
```kotlin
client.uploadFile(
    fileName = "test.jpg",
    fileUrl = "file:///tmp/test.jpeg",
    credentials = Credentials("token, "tokenSecret")
)
```

*Java:*
```java
client.uploadFile("test.jpg", "file:///tmp/test.jpeg", new Credentials("token, "tokenSecret"));
```

**NOTE:** You can not specify a Credentials Provider in a  `Partner`
App but you do not need to pass `Credentials` to a client call.


## Usage

```java
final ObjectMapper objectMapper = ConfigUtilsKt.configureObjectMapper(new Jackson2ObjectMapperBuilder()).build();

final Http http = ConfigUtilsKt.configureHttp(
        Http.builder().requestFactory(
                new RestAsyncClientHttpRequestFactory(httpClient, executor)),
                objectMapper
)
.build();

final Config config = Config.Companion.getPublicAppConfig(
   "<ConsumerKey>",
   "<ConsumerSecret>",
   "My-Fancy-User-Agent",
   () -> new Credentials("<token>", "<tokenSecret>")
);

final Files.Client client = new Files.Client(http, config);

final Credentials credentials = new Credentials("<token>", "<tokenSecret>");

final CompletableFuture<Files.FileDto> future = client.uploadFile("test.jpg", "file:///tmp/test.jpeg");
final Files.FileDto file = Completion.join(future);


final Associations.Client associationsClient = new Associations.Client(http, config);
Completion.join(associationsClient.createAssociation(
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


[![](https://jitpack.io/v/xamatech/xama-xero-client.svg)](https://jitpack.io/#xamatech/xama-xero-client)
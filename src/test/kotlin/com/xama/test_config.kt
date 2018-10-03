package com.xama

import com.xama.client.AppType
import com.xama.client.Config
import com.xama.client.Credentials
import java.net.URL

object TestConfig {

    val testImageUrl = URL("file:///Users/bfriedrich/Desktop/test.jpeg")

    val testConfig = Config(
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
}

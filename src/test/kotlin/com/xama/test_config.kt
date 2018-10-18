package com.xama

import com.xama.client.Config
import com.xama.client.Credentials

object TestConfig {

    /*

    val testConfig = Config.getPrivateAppConfig(
            consumerKey = "...",
            consumerSecret = "...",
            privateKeyCert = "/public_privatekey.pfx"
    )
    */


    val testConfig = Config.getPublicAppConfig(
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

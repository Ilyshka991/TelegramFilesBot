package com.pechuro.guitarbot.app

import com.github.kotlintelegrambot.logging.LogLevel
import java.util.*

object Configuration {

    private val props = Properties().apply {
        load(Thread.currentThread().contextClassLoader.getResourceAsStream("apikeys.properties"))
    }

    object App {
        val applicationName: String = props.getProperty("application.name")
        const val maxFilesPerPage = 6
    }

    object Google {
        val googleServiceAccountEmail: String = props.getProperty("google.service_account_email")
        val googleCredentialsFilePath: String = props.getProperty("google.credentials_file")
        val googleRootFilePath: String = props.getProperty("google.root_file_path")
    }

    object Telegram {
        val token: String = props.getProperty("telegram.bot_token")
        const val timeoutSec = 30
        val logLevel = LogLevel.Network.Body
    }
}

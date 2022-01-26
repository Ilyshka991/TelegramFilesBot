package com.pechuro.guitarbot.app

import com.github.kotlintelegrambot.logging.LogLevel

object Configuration {

    object App {
        val APPLICATION_NAME: String = System.getenv("application.name")
        const val MAX_FILES_PER_PAGE = 6
    }

    object Google {
        val API_KEY: String = System.getenv("google.api_key_json")
        val ROOT_FILE_PATH: String = System.getenv("google.root_file_path")
    }

    object Telegram {
        val TOKEN: String = System.getenv("telegram.bot_token")
        const val TIMEOUT_SEC = 30
        val LOG_LEVEL = LogLevel.Network.Body
    }
}

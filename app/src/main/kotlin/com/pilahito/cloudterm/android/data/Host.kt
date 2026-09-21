package com.pilahito.cloudterm.android.data

import java.util.UUID

enum class AuthType { PASSWORD, KEY }

enum class Protocol {
    SSH,
    SFTP,
    FTP,
    FTPS,
    ;

    val defaultPort: Int
        get() = when (this) {
            SSH, SFTP -> 22
            FTP -> 21
            FTPS -> 990
        }

    val hasTerminal: Boolean
        get() = this == SSH

    val label: String
        get() = when (this) {
            SSH -> "SSH"
            SFTP -> "SFTP"
            FTP -> "FTP"
            FTPS -> "FTPS"
        }
}

data class Host(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType = AuthType.PASSWORD,
    val protocol: Protocol = Protocol.SSH,
)

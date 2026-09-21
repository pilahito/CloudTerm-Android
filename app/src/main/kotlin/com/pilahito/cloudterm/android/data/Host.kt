package com.pilahito.cloudterm.android.data

import java.util.UUID

enum class AuthType { PASSWORD, KEY }

data class Host(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType = AuthType.PASSWORD,
)

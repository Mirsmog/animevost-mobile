package com.animevost.app.core.domain.model

data class User(
    val id: Int,
    val name: String,
    val avatarUrl: String,
    val isLoggedIn: Boolean,
)

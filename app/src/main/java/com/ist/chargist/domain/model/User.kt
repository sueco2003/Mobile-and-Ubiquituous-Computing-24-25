package com.ist.chargist.domain.model

data class User(
    val userId: String = "",
    val username: String = "",
    val favourites : List<String>?
)

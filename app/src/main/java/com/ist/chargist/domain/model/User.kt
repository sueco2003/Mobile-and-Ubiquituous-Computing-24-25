package com.ist.chargist.domain.model

data class User(
    val userId: String = "",
    val userEmail: String = "",
    val favourites : List<String>
)

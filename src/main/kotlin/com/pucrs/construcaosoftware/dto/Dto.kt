package com.pucrs.construcaosoftware.dto

data class UserDTO(
    val id: String,
    val username: String,
    val role: String? = null,
    val email: String? = null,
)

data class UserCreateDTO(
    val username: String,
    val role: String? = null,
    val email: String? = null,
    val password: String? = null
)

data class UserUpdateDTO(
    val role: String? = null,
    val email: String? = null,
)

data class UserPartialUpdateDTO(val password: String)

data class SuccessDTO(val success: Boolean? = false)

data class TokenDTO(
    val access_token: String? = null,
    val expires_in: Int? = null
)

data class LoginDTO(
  val username: String,
  val password: String,
)
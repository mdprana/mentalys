package com.mentalys.app.utils

open class Resource<out R> private constructor() {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val error: String) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
    data object Empty : Resource<Nothing>()
    data class Unauthorized(val error: String = "Session expired. Please login again.") : Resource<Nothing>()
    data class NetworkTimeout(val error: String = "Connection timed out. Please try again.") : Resource<Nothing>()
    data class InvalidResponse(val error: String = "Invalid response from server.") : Resource<Nothing>()
}

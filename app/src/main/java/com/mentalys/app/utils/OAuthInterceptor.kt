package com.mentalys.app.utils

import okhttp3.Interceptor
import okhttp3.Response

class OAuthInterceptor(
    private val accessToken: String,
    private val scheme: String = "Bearer"
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val authorizedRequest = originalRequest.newBuilder()
            .header("Authorization", "$scheme $accessToken")
            .build()
        return chain.proceed(authorizedRequest)
    }
}

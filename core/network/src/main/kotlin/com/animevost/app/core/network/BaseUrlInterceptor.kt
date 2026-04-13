package com.animevost.app.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites the host of every outgoing animevost/mirror request to the currently
 * active base URL resolved by [EndpointResolver].
 * All other hosts (Jikan, GitHub, etc.) are passed through unchanged.
 */
@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val resolver: EndpointResolver,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (!isAnimevostHost(original.url.host)) return chain.proceed(original)

        val activeBase = resolver.currentBaseUrl.toHttpUrlOrNull()
            ?: return chain.proceed(original)

        val newUrl = original.url.newBuilder()
            .scheme(activeBase.scheme)
            .host(activeBase.host)
            .port(activeBase.port)
            .build()

        return chain.proceed(original.newBuilder().url(newUrl).build())
    }

    private fun isAnimevostHost(host: String) =
        host.contains("animevost") || host.contains("vost.pw")
}

package dev.kmpapp.data.remote.network.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import dev.kmpapp.common.Either

/**
 * Created by Ali Sadeghi
 * on 15,Apr,2025
 */

class ApiClient(
    val client: HttpClient,
) {
    val callAdapterFactory = CallAdapterFactory()

    suspend inline fun <reified T> executeCall(noinline requestBuilder: suspend () -> HttpResponse): Either<T> {
        val callAdapter = callAdapterFactory.create<T>()

        return callAdapter.adapt(requestBuilder)
    }

    // Fixed helper methods for common HTTP methods
    suspend inline fun <reified T, reified R : Any> get(
        resource: R,
        requestConfigs: List<RequestConfig> = RequestConfig.build(),
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Either<T> =
        executeCall {
            client.get(resource) {
                attributes.put(RequestConfigKey, requestConfigs)
                block()
            }
        }

    suspend inline fun <reified T, reified R : Any> post(
        resource: R,
        body: Any? = null,
        requestConfigs: List<RequestConfig> = RequestConfig.build(),
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Either<T> =
        executeCall {
            client.post(resource) {
                attributes.put(RequestConfigKey, requestConfigs)

                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                block()
            }
        }

    suspend inline fun <reified T, reified R : Any> put(
        resource: R,
        body: Any? = null,
        requestConfigs: List<RequestConfig> = RequestConfig.build(),
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Either<T> =
        executeCall {
            client.put(resource) {
                attributes.put(RequestConfigKey, requestConfigs)

                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                block()
            }
        }

    suspend inline fun <reified T, reified R : Any> delete(
        resource: R,
        requestConfigs: List<RequestConfig> = RequestConfig.build(),
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Either<T> =
        executeCall {
            client.delete(resource) {
                attributes.put(RequestConfigKey, requestConfigs)

                block()
            }
        }
}

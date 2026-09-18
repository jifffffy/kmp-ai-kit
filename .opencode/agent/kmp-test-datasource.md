---
description: Generates DataSource tests using MockEngine.
mode: subagent
---

# DataSource Test Agent

Test DataSource implementations using Ktor MockEngine. **Do NOT re-read source files** - use provided context.

> **Scope (Rule 14)**: this agent covers **REST** DataSources (Ktor `ApiClient` + MockEngine) only. A **platform** DataSource (GPS/camera/biometrics behind an `expect/actual` interface) has no MockEngine and no HTTP — do not template it here. Platform capabilities are exercised by **faking the DataSource interface** (Mokkery) at the Repository/ViewModel level via `test-repository` / `test-viewmodel`; the per-platform `actual` classes are not unit-tested in `commonTest`.

## Output Path
```
feature/{featurename}/src/commonTest/kotlin/{PKG_PATH}/{featurename}/data/datasource/{Feature}RemoteDataSourceTest.kt
```

## Template

```kotlin
package {PKG_PREFIX}.{featurename}.data.datasource

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import {CORE_COMMON_PKG}.Either
import {CORE_COMMON_PKG}.ErrorModel
import {CORE_DATA_PKG}.ErrorConst
import {CORE_DATA_PKG}.ApiClient
import {PKG_PREFIX}.{featurename}.fixtures.{Feature}Fixtures
import kotlin.test.*

class {Feature}RemoteDataSourceTest {

    private lateinit var mockEngine: MockEngine

    private fun createDataSource(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): {Feature}RemoteDataSource {
        mockEngine = MockEngine { request -> handler(request) }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Resources)
        }
        val apiClient = ApiClient(client)
        return {Feature}RemoteDataSourceImpl(apiClient)
    }

    // === SUCCESS CASES ===

    @Test
    fun `get{Entity} returns success when API returns 200`() = runTest {
        val dataSource = createDataSource { request ->
            respond(
                content = {Feature}Fixtures.valid{Entity}Json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val result = dataSource.get{Entity}()

        assertTrue(result is Either.Success)
    }

    // === HTTP ERROR CODES ===
    // Error JSONs use {"detail": "...", "code": ...} format (NetworkErrorModel)

    @Test
    fun `get{Entity} returns failure on 401 Unauthorized`() = runTest {
        val dataSource = createDataSource { request ->
            respond(
                content = {Feature}Fixtures.error401Json,
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val result = dataSource.get{Entity}()

        assertTrue(result is Either.Failure)
        val error = (result as Either.Failure).error as ErrorModel.MessageCode
        assertEquals("You must login", error.message)
        assertEquals(1001, error.code)
    }

    @Test
    fun `get{Entity} returns failure on 404 Not Found`() = runTest {
        val dataSource = createDataSource { request ->
            respond(
                content = {Feature}Fixtures.error404Json,
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val result = dataSource.get{Entity}()

        assertTrue(result is Either.Failure)
        val error = (result as Either.Failure).error as ErrorModel.MessageCode
        assertEquals("{Resource} not found", error.message)
        assertEquals(404, error.code)
    }

    @Test
    fun `get{Entity} returns failure on 500 Internal Server Error`() = runTest {
        val dataSource = createDataSource { request ->
            respond(
                content = {Feature}Fixtures.error500Json,
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val result = dataSource.get{Entity}()

        assertTrue(result is Either.Failure)
        val error = (result as Either.Failure).error as ErrorModel.MessageCode
        assertEquals("Internal Server Error", error.message)
        assertEquals(5001, error.code)
    }

    @Test
    fun `get{Entity} returns failure on 503 Service Unavailable`() = runTest {
        val dataSource = createDataSource { request ->
            respond(
                content = {Feature}Fixtures.error503Json,
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val result = dataSource.get{Entity}()

        assertTrue(result is Either.Failure)
        val error = (result as Either.Failure).error as ErrorModel.MessageCode
        assertEquals("An unknown network error has occurred!", error.message)
        assertEquals(503, error.code)
    }

    // === NETWORK FAILURES ===

    @Test
    fun `get{Entity} returns failure on connection error`() = runTest {
        val dataSource = createDataSource { _ ->
            throw Exception("Connection refused")
        }

        val result = dataSource.get{Entity}()

        assertTrue(result is Either.Failure)
        assertEquals(ErrorConst.NoNetwork, (result as Either.Failure).error)
    }

    // === REQUEST VERIFICATION ===

    @Test
    fun `get{Entity} sends correct request path`() = runTest {
        var capturedUrl: String? = null
        val dataSource = createDataSource { request ->
            capturedUrl = request.url.toString()
            respond(
                content = {Feature}Fixtures.valid{Feature}ResponseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        dataSource.get{Entity}()

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("/api/v1/{endpoint}/"))
    }
}
```

## Checklist

**Success:** 200 single entity | 200 list | 200 empty list | 200 paginated (if applicable)

**HTTP Errors (copy template, change status):** 400 | 401 | 403 | 404 | 500 | 503

**Parsing:** Malformed JSON → SerializationError | Empty body → SerializationError | Missing fields → SerializationError | Null optionals → success | Extra fields → ignored

**Network:** Connection refused → NoNetwork | Timeout → NoNetwork | Unknown host → NoNetwork

**Request Verification:** Correct URL | Correct HTTP method | Auth header | Content-Type | Request body | Query params

## Verify
```bash
./gradlew :feature:{featurename}:cleanDesktopTest :feature:{featurename}:desktopTest --tests "*RemoteDataSourceTest"
```

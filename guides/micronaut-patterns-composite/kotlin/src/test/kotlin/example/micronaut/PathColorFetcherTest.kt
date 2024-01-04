/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.micronaut

import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Optional

@Property(name = "spec.name", value = "PathColorFetcherTest") // <1>
@MicronautTest
class PathColorFetcherTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient // <2>

    @Test
    fun theHttpHeaderColorFetcherFetchesFromColorHeader() {
        val client = httpClient.toBlocking()
        assertEquals("mint", client.retrieve(HttpRequest.GET<Any>("/colorpath/mint")))
        assertThrows(HttpClientResponseException::class.java) {
            client.retrieve(HttpRequest.GET<Any>("/colorpath/foo"))
        }
    }

    @Requires(property = "spec.name", value = "PathColorFetcherTest") // <1>
    @Controller("/colorpath")
    class PathColorFetcherTestController(private val colorFetcher: PathColorFetcher) {

        @Produces(MediaType.TEXT_PLAIN)
        @Get("/mint")
        fun index(request: HttpRequest<*>): Optional<String> =
            colorFetcher.favouriteColor(request)

        @Produces(MediaType.TEXT_PLAIN)
        @Get("/foo")
        fun foo(request: HttpRequest<*>): Optional<String> =
            colorFetcher.favouriteColor(request)
    }
}

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

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import jakarta.inject.Inject
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import reactor.core.publisher.Flux

@MicronautTest
class BooksControllerTest {
    @Inject
    @field:Client("/")
    lateinit var client:HttpClient

    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    @Test
    fun testRetrieveBooks() {
        val books = client.toBlocking().retrieve(HttpRequest.GET<Any>("/books"),
            Argument.listOf(BookRecommendation::class.java))
        assertEquals(books.size, 1)
        assertEquals(books[0].name, "Building Microservices")
    }
}

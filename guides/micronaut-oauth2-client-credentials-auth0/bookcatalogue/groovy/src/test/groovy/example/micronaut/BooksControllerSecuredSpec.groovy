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

import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
import static io.micronaut.http.HttpStatus.UNAUTHORIZED
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import jakarta.inject.Inject

@MicronautTest
class BooksControllerSecuredSpec extends Specification {
    @Inject
    @Client('/')
    HttpClient httpClient

    void '/books is secured'() {
        when:
        httpClient.toBlocking().exchange(HttpRequest.GET('/books'))
        
        then:
        HttpClientResponseException e = thrown()
        UNAUTHORIZED == e.status
    }
}

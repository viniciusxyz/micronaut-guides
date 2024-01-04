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

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS
import io.micronaut.security.rules.SecurityRule.IS_AUTHENTICATED
import io.micronaut.views.View

@Controller // <1>
class HomeController {

    @Secured(IS_ANONYMOUS) // <2>
    @View("home") // <3>
    @Get // <4>
    fun index(): Map<String, Any> = mapOf()

    @Secured(IS_AUTHENTICATED) // <5>
    @Get("/secure") // <6>
    fun secured(): Map<String, Any> = mapOf("secured" to true) // <7>
}

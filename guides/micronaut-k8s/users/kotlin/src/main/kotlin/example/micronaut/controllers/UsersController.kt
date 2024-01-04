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
package example.micronaut.controllers

import example.micronaut.models.User
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import kotlin.collections.ArrayList

@Controller("/users") // <1>
@Secured(SecurityRule.IS_AUTHENTICATED) // <2>
open class UsersController {
    var persons: MutableList<User?> = ArrayList()

    @Post // <3>
    open fun add(@Body user: @Valid User?): User {
        val foundUser = findByUsername(user!!.username)
        if (foundUser != null) {
            throw HttpStatusException(HttpStatus.CONFLICT, "User with provided username already exists")
        }
        val newUser = User(persons.size + 1, user.firstName, user.lastName, user.username)
        persons.add(newUser)
        return newUser
    }

    @Get("/{id}") // <4>
    open fun findById(id: @NotNull Int?): User? {
        return persons
            .firstOrNull { it: User? ->
                it!!.id == id
            }
    }

    @Get // <5>
    open fun getUsers(): List<User?>? {
        return persons
    }

    open fun findByUsername(username: @NotNull String?): User? {
        return persons.firstOrNull { it: User? ->
            it!!.username == username
        }
    }
}
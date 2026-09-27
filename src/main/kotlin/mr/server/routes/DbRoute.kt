package mr.server.routes

import mr.server.db.ensureDatabase
import mr.server.db.entity.user.User
import mr.server.db.entity.user.UserRepository
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.routes

// JSON shape: dates as ISO strings, so Jackson needs no JavaTime module.
data class UserDto(val id: Int, val name: String, val createdAt: String)
data class CreateUser(val name: String)

private val userListLens = Body.auto<List<UserDto>>().toLens()
private val userLens = Body.auto<UserDto>().toLens()
private val createUserLens = Body.auto<CreateUser>().toLens()

private fun toDto(user: User) =
    UserDto(user.id, user.name, user.createdAt.toString())

object DbRoute {
    private val listUsers = "/db/users" bind GET to {
        Response(OK).with(userListLens of UserRepository.all().map(::toDto))
    }

    private val createUser = "/db/users" bind POST to { request ->
        val body = createUserLens(request)
        Response(CREATED).with(userLens of toDto(UserRepository.create(body.name)))
    }

    val handlers = ensureDatabase.then(
        routes(
            listUsers,
            createUser
        )
    )
}

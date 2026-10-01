package mr.server.routes

import mr.server.db.ensureDatabase
import mr.server.db.entity.user.User
import mr.server.db.entity.user.UserRepository
import mr.server.formats.AppJson.auto
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.routing.bind
import org.http4k.routing.routes
import kotlin.time.Instant

data class UserDto(val id: Int, val name: String, val createdAt: Instant)
data class CreateUser(val name: String)

private val userListLens = Body.auto<List<UserDto>>().toLens()
private val userLens = Body.auto<UserDto>().toLens()
private val createUserLens = Body.auto<CreateUser>().toLens()

private fun toDto(user: User) = UserDto(user.id, user.name, user.createdAt)

object UserRoute {
    private const val PATH = "/users"

    private val listUsers = PATH bind GET to {
        Response(OK).with(userListLens of UserRepository.all().map(::toDto))
    }

    private val createUser = PATH bind POST to { request ->
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

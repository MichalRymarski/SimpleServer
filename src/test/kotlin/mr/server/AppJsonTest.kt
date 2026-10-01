package mr.server

import mr.server.formats.AppJson.auto
import mr.server.routes.UserDto
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class AppJsonTest {

    private val userLens = Body.auto<UserDto>().toLens()

    @Test
    fun `instant serializes as ISO-8601 string`() {
        val dto = UserDto(1, "jim", Instant.parse("2026-09-27T13:15:58.548225Z"))

        val body = Response(OK).with(userLens of dto).bodyString()

        check(body.contains(""""createdAt":"2026-09-27T13:15:58.548225Z"""")) {
            "unexpected body: $body"
        }
    }

    @Test
    fun `instant round-trips`() {
        val dto = UserDto(1, "jim", Instant.parse("2026-09-27T13:15:58.548225Z"))

        val parsed = userLens(Response(OK).with(userLens of dto))

        check(parsed == dto) { "round-trip failed: $parsed" }
    }
}

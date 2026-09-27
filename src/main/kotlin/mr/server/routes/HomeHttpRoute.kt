package mr.server.routes

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind

object HomeHttpRoute {
    private const val path = "/"

    val handler = path bind GET to { _ ->
        Response(OK).body("SimpleServer up. Try /ping, /formats/json/jackson, /contract/api/v1/swagger.json, /mcp")
    }
}
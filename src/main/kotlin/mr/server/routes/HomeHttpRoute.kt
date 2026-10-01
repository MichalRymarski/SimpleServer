package mr.server.routes

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes

object HomeHttpRoute {
    private const val PATH = "/"

    private val get = PATH bind GET to { _ ->
        Response(OK).body("SimpleServer up. Try /ping, /formats/json/jackson, /contract/api/v1/swagger.json, /mcp")
    }
    private val post = PATH bind POST to {
        Response(OK).body("SimpleServer up. Try /ping, /formats/json/jackson, /contract/api/v1/swagger.json, /mcp")
    }

    val handlers = routes(
        get,
        post
    )
}
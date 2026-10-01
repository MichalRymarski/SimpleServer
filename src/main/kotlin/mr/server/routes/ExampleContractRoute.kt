package mr.server.routes

import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.auto
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

data class NameAndMessage(val name: String, val message: String)
// the body lens here is imported as an extension function from the Jackson instance
val nameAndMessageLens = Body.auto<NameAndMessage>().toLens()

object ExampleContractRoute {
    // this specifies the route contract, including examples of the input and output body objects - they will
    // get exploded into JSON schema in the OpenAPI docs
    private val spec = "/echo" meta {
        summary = "echoes the name and message sent to it"
        receiving(nameAndMessageLens to NameAndMessage("jim", "hello!"))
        returning(OK, nameAndMessageLens to NameAndMessage("jim", "hello!"))
    } bindContract POST

    // deliberately path- and method-agnostic: the contract route invokes this with whatever
    // path/method it was mounted under, so it must not re-route
    private val echo: HttpHandler = { request ->
        val received: NameAndMessage = nameAndMessageLens(request)
        Response(OK).with(nameAndMessageLens of received)
    }

    // standalone binding for the main app, built from the same handler
    val handlers: RoutingHttpHandler = "/echo" bind Method.GET to echo

    operator fun invoke(): ContractRoute = spec to echo
}

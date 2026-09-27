package mr.server

import io.github.oshai.kotlinlogging.KotlinLogging
import mr.server.formats.imageFile
import mr.server.formats.nameField
import mr.server.formats.precompiledJteRenderer
import mr.server.formats.strictFormBody
import mr.server.models.TestViewModel
import mr.server.routes.DbRoute
import mr.server.routes.ExampleContractRoute
import mr.server.routes.HomeHttpRoute
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.*
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.DigestAuth
import org.http4k.filter.ServerFilters
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.ApiKeySecurity
import org.http4k.security.Nonce.Companion.SECURE_NONCE
import org.http4k.security.digest.DigestAlgorithm.MD5
import org.http4k.template.viewModel

private val Log = KotlinLogging.logger {}

private val handleLensFailure: Filter = ServerFilters.CatchLensFailure { request, lensFailure ->
    Log.warn { "Bad request ${request.method} ${request.uri}: ${lensFailure.failures.joinToString("; ")}" }
    Response(BAD_REQUEST.description(lensFailure.failures.joinToString("; ")))
}

private val catchAll: Filter = ServerFilters.CatchAll { throwable ->
    Log.error(throwable) { "Unhandled exception" }
    Response(INTERNAL_SERVER_ERROR).body("Internal Server Error")
}

private val router: HttpHandler = routes(
    HomeHttpRoute.handlers,
    ExampleContractRoute.handler,
    DbRoute.handlers,
    "/formats/multipart" bind POST to { request ->
        // to extract the contents, we first extract the form and then extract the fields from it using the lenses
        // NOTE: we are "using" the form body here because we want to close the underlying file streams
        strictFormBody(request).use {
            println(nameField(it))
            println(imageFile(it))
        }

        Response(OK)
    },

    "/templates/jte/test" bind GET to {
        val view = Body.viewModel(precompiledJteRenderer, TEXT_HTML).toLens()
        Response(OK).with(view of TestViewModel(1, null))
    },

    "/digest" bind ServerFilters.DigestAuth(
        "realm", { it }, nonceGenerator = SECURE_NONCE, nonceVerifier = { true }, algorithm = MD5
    ).then { Response(OK).body("hello!") },

    "/contract/api/v1" bind contract {
        renderer = OpenApi3(ApiInfo("SimpleServer API", "v1.0"))
        descriptionPath = "/swagger.json"
        security = ApiKeySecurity(Query.int().required("api"), { it == 42 }) // Allow only requests with &api=42
        routes += ExampleContractRoute()
    }
)

val app: HttpHandler = catchAll.then(handleLensFailure.then(router))
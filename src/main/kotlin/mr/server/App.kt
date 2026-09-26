package mr.server

import io.github.oshai.kotlinlogging.KotlinLogging
import mr.server.formats.*
import mr.server.models.JTEViewModel
import mr.server.models.TestViewModel
import mr.server.routes.ExampleContractRoute
import org.http4k.client.JavaHttpClient
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.*
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.DigestAuth
import org.http4k.filter.ServerFilters
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.ApiKeySecurity
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.Nonce.Companion.SECURE_NONCE
import org.http4k.security.OAuthProvider
import org.http4k.security.digest.DigestAlgorithm.MD5
import org.http4k.security.google
import org.http4k.template.viewModel

private val Log = KotlinLogging.logger {}

// Google OAuth Example
// Browse to: http://localhost:9000/oauth - you'll be redirected to google for authentication
private val googleClientId = "myGoogleClientId"
private val googleClientSecret = "myGoogleClientSecret"

// this is a test implementation of the OAuthPersistence interface, which should be
// implemented by application developers
private val oAuthPersistence = InsecureCookieBasedOAuthPersistence("Google")

// pre-defined configuration exist for common OAuth providers
private val oauthProvider = OAuthProvider.google(
    JavaHttpClient(),
    Credentials(googleClientId, googleClientSecret),
    Uri.of("http://localhost:9000/oauth/callback"),
    oAuthPersistence
)

val app: HttpHandler = routes(
    "/" bind GET to {
        Response(OK).body("SimpleServer up. Try /ping, /formats/json/jackson, /contract/api/v1/swagger.json, /mcp")
    },

    "/ping" bind GET to {
        Log.info { "Ping request received" }
        Response(OK).body("pong")
    },

    "/formats/multipart" bind POST to { request ->
        // to extract the contents, we first extract the form and then extract the fields from it using the lenses
        // NOTE: we are "using" the form body here because we want to close the underlying file streams
        strictFormBody(request).use {
            println(nameField(it))
            println(imageFile(it))
        }

        Response(OK)
    },

    "/formats/json/jackson" bind GET to {
        Response(OK).with(jacksonMessageLens of JacksonMessage("Barry", "Hello there!"))
    },

    "/templates/jte" bind GET to {
        val view = Body.viewModel(precompiledJteRenderer, TEXT_HTML).toLens()
        val viewModel = JTEViewModel("Hello there!")
        Response(OK).with(view of viewModel)
    },

    "/templates/jte/test" bind GET to {
        val view = Body.viewModel(precompiledJteRenderer, TEXT_HTML).toLens()
        Response(OK).with(view of TestViewModel(1, null))
    },

    "/testing/kotest" bind GET to { request ->
        Response(OK).body("Echo '${request.bodyString()}'")
    },

    "/oauth" bind routes(
        "/" bind GET to oauthProvider.authFilter.then { Response(OK).body("hello!") },
        "/callback" bind GET to oauthProvider.callback
    ),

    "/digest" bind ServerFilters.DigestAuth(
        "realm",
        { it },
        nonceGenerator = SECURE_NONCE,
        nonceVerifier = { true },
        algorithm = MD5
    )
        .then { Response(OK).body("hello!") },

    "/contract/api/v1" bind contract {
        renderer = OpenApi3(ApiInfo("SimpleServer API", "v1.0"))

        // Return Swagger API definition under /contract/api/v1/swagger.json
        descriptionPath = "/swagger.json"

        // You can use security filter tio protect routes
        security = ApiKeySecurity(Query.int().required("api"), { it == 42 }) // Allow only requests with &api=42

        // Add contract routes
        routes += ExampleContractRoute()
    }
)
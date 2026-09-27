package mr.server.routes

import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.google

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

object OAuthRoute {
    val oauth = "/oauth" bind routes(
        "/" bind Method.GET to oauthProvider.authFilter.then { Response(OK).body("hello!") },
        "/callback" bind Method.GET to oauthProvider.callback
    )
}
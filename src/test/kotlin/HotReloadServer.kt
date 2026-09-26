import mr.server.app
import org.http4k.core.HttpHandler
import org.http4k.hotreload.HotReloadServer
import org.http4k.hotreload.HotReloadable
import org.http4k.server.SunHttp

class ReloadableHttpApp : HotReloadable<HttpHandler> {
    override fun create() = app
}

fun main() {
    val port = System.getProperty("hotreload.port", "8000").toInt()
    HotReloadServer.http<ReloadableHttpApp>(SunHttp(port)).start()
}

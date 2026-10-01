package mr.server

import mr.server.db.closeDatabase
import mr.server.db.initDatabase
import mr.server.mcp.simpleServerMcpStreaming
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.server.Helidon
import org.http4k.server.Http4kServer
import org.http4k.server.asServer
import java.net.BindException

inline fun <reified T : Throwable> Throwable.findCause(): T? =
    generateSequence(this) { it.cause }.filterIsInstance<T>().firstOrNull()

fun startOrNextPort(startPort: Int, start: (Int) -> Http4kServer): Http4kServer {
    var port = startPort
    while (true) {
        return runCatching { start(port) }.recoverCatching { e ->
            e.findCause<BindException>() ?: throw e  // not a bind conflict → rethrow
            println("Port $port in use, trying ${port + 1}")
            start(++port)
        }.getOrThrow()
    }
}

fun main() {
    initDatabase()
    Runtime.getRuntime().addShutdownHook(Thread { closeDatabase() })

    val printingApp: HttpHandler = PrintRequest().then(app)

    val server = startOrNextPort(9000) { PolyHandler(printingApp, wsApp).asServer(Helidon(it)).start() }
    val mcpServer = startOrNextPort(9001) { simpleServerMcpStreaming().asServer(Helidon(it)).start() }
    println("Server started on " + server.port())
    println("MCP server started on " + mcpServer.port() + " (/mcp)")
}

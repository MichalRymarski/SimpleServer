package mr.server.mcp

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.HttpHandler
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.http4k.routing.mcpHttpNonStreaming

val nameArg = Tool.Arg.string().required("name", "name to echo")
val messageArg = Tool.Arg.string().required("message", "message to echo")

val pingTool = Tool("ping", "health check - returns pong") bind {
    ToolResponse.Ok(Content.Text("pong"))
}

val echoTool = Tool("echo", "echoes back the name and message sent to it", nameArg, messageArg) bind { req ->
    ToolResponse.Ok(Content.Text("${nameArg(req)}: ${messageArg(req)}"))
}

fun simpleServerMcp(): HttpHandler = mcpHttpNonStreaming(
    ServerMetaData(McpEntity.of("SimpleServer"), Version.of("1.0.0")),
    NoMcpSecurity,
    pingTool,
    echoTool
)

// Full streaming MCP (HTTP Streaming on /mcp + SSE on /sse). Owns its paths,
// so serve it as a separate server (e.g. port 9001), not nested in routes().
fun simpleServerMcpStreaming() = mcp(
    ServerMetaData(McpEntity.of("SimpleServer"), Version.of("1.0.0")),
    NoMcpSecurity,
    pingTool,
    echoTool
)

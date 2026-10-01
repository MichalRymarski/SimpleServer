package mr.server.utils

import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage

fun Websocket.send(message: String) {
    this.send(WsMessage(message))
}
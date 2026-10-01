@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package mr.server.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import mr.server.db.entity.chat.ChatMessage
import mr.server.db.entity.chat.ChatRepository
import mr.server.db.entity.chat.ChatRepository.sendMessage
import mr.server.db.entity.chat.ChatRoom
import mr.server.formats.AppJson.auto
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.LensFailure
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.bindWs
import org.http4k.routing.routes
import org.http4k.routing.websockets
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.REFUSE
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class ChatDto(val uuid: Uuid, val userOne: Int, val userTwo: Int)
data class CreateChat(val userOne: Int, val userTwo: Int)
data class MessageDto(
    val id: Int,
    val chatUuid: Uuid,
    val senderId: Int,
    val text: String,
    val createdAt: Instant?
)

data class SendMessage(val senderId: Int, val text: String)
data class WsIncoming(val senderId: Int, val text: String)

private val chatLens = Body.auto<ChatDto>().toLens()
private val createChatLens = Body.auto<CreateChat>().toLens()
private val chatListLens = Body.auto<List<ChatDto>>().toLens()
private val messageLens = Body.auto<MessageDto>().toLens()
private val messageListLens = Body.auto<List<MessageDto>>().toLens()
private val sendMessageLens = Body.auto<SendMessage>().toLens()
private val wsMessageLens = WsMessage.auto<MessageDto>().toLens()
private val wsIncomingLens = WsMessage.auto<WsIncoming>().toLens()
private val Log = KotlinLogging.logger {}

private fun toDto(room: ChatRoom) = ChatDto(room.uuid, room.userOne, room.userTwo)

private fun toDto(message: ChatMessage) = MessageDto(
    message.id, message.chatUuid, message.senderId, message.text, message.createdAt
)

object ChatRoute {
    private val createChat = "/chat" bind POST to { request ->
        val body = createChatLens(request)
        Response(OK).with(chatLens of toDto(ChatRepository.getOrCreate(body.userOne, body.userTwo)))
    }

    private val sendMessage = "/chat/{uuid}/messages" bind POST to { request ->
        val chatUuid = Uuid.parse(Path.of("uuid")(request))
        val body = sendMessageLens(request)
        val sent = sendMessage(chatUuid, body.senderId, body.text)
        Response(CREATED).with(messageLens of toDto(sent))
    }

    private val listMessages = "/chat/{uuid}/messages" bind GET to { request ->
        val chatUuid = Uuid.parse(Path.of("uuid")(request))
        Response(OK).with(messageListLens of ChatRepository.messages(chatUuid).map(::toDto))
    }

    private val listChats = "/chat" bind GET to { request ->
        val user = Query.int().required("id")(request)
        Response(OK).with(chatListLens of ChatRepository.getAllUserChats(user).map(::toDto))
    }

    val websocket = websockets(
        "/chat/{uuid}" bindWs { request ->
            val chatUuid = try {
                Uuid.parse(Path.of("uuid")(request))
            } catch (e: IllegalArgumentException) {
                Log.error { "Invalid UUID: ${e.message}" }
                return@bindWs WsResponse { ws -> ws.close(REFUSE) }
            }
            WsResponse { ws ->
                ws.onMessage {
                    try {
                        val incoming = wsIncomingLens(it)
                        val created = sendMessage(chatUuid, incoming.senderId, incoming.text)
                        ws.send(wsMessageLens(toDto(created)))
                    } catch (e: LensFailure) {
                        Log.error { "Failed to process incoming message: ${e.message}" }
                        ws.send(WsMessage("""{"error":"bad frame"}"""))
                    } catch (e: IllegalArgumentException) {
                        Log.error { "Invalid UUID: ${e.message}" }
                        ws.send(WsMessage("""{"error":"${e.message}"}"""))
                    }
                }
                ws.onClose {
                    Log.info { "Close chat message" }
                }
            }
        }
    )

    val handlers = routes(
        createChat,
        sendMessage,
        listMessages,
        listChats
    )
}

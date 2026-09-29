package mr.server.db.entity.chat

import mr.server.db.entity.chat.Chat.userOne
import mr.server.db.entity.chat.Chat.userTwo
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Clock
import kotlin.time.Instant

data class ChatRoom(val userOne: Int, val userTwo: Int)
data class ChatMessage(
    val id: Int,
    val userOne: Int,
    val userTwo: Int,
    val senderId: Int,
    val text: String,
    val createdAt: Instant
)


object ChatRepository {
    fun getAllUserChats(userId: Int): List<ChatRoom> {
        return Chat
            .selectAll()
            .where { (userOne eq userId) or (userTwo eq userId) }
            .map { ChatRoom(it[userOne], it[userTwo]) }
    }

    fun getAllMessagesForChat(userOneId: Int, userTwoId: Int): List<ChatMessage> {
        return Message
            .selectAll()
            .where { (Message.userOne eq userOneId) and (Message.userTwo eq userTwoId) }
            .map {
                ChatMessage(
                    it[Message.id],
                    it[Message.userOne],
                    it[Message.userTwo],
                    it[Message.senderId],
                    it[Message.text],
                    it[Message.createdAt]
                )
            }
    }

    fun createMessage(userOneId: Int, userTwoId: Int, senderId: Int, text: String): ChatMessage {
        val now = Clock.System.now()
        val id = Message.insert {
            it[Message.userOne] = userOneId
            it[Message.userTwo] = userTwoId
            it[Message.senderId] = senderId
            it[Message.text] = text
            it[Message.createdAt] = now
        } get Message.id

        return ChatMessage(id, userOneId, userTwoId, senderId, text, now)
    }

    fun createChatIfNotExists(userOneId: Int, userTwoId: Int): ChatRoom {
        val existingChat = Chat
            .selectAll()
            .where { (userOne eq userOneId) and (userTwo eq userTwoId) }
            .firstOrNull()

        if (existingChat != null) {
            return ChatRoom(existingChat[userOne], existingChat[userTwo])
        }

        Chat.insert {
            it[Chat.userOne] = userOneId
            it[Chat.userTwo] = userTwoId
        }

        return ChatRoom(userOneId, userTwoId)
    }
}

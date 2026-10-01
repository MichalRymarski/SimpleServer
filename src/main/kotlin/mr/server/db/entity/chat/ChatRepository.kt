@file:OptIn(ExperimentalUuidApi::class)

package mr.server.db.entity.chat

import mr.server.db.entity.chat.Chat.userOne
import mr.server.db.entity.chat.Chat.userTwo
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ChatRoom(val uuid: Uuid, val userOne: Int, val userTwo: Int)
data class ChatMessage(
    val id: Int,
    val chatUuid: Uuid,
    val senderId: Int,
    val text: String,
    val createdAt: Instant
)

object ChatRepository {
    private fun ordered(userA: Int, userB: Int): Pair<Int, Int> {
        require(userA != userB) { "a chat needs two different users" }
        return if (userA < userB) userA to userB else userB to userA
    }

    fun getOrCreate(userA: Int, userB: Int): ChatRoom {
        val (lo, hi) = ordered(userA, userB)
        return transaction {
            Chat.insertIgnore {
                it[Chat.uuid] = Uuid.random()
                it[Chat.userOne] = lo
                it[Chat.userTwo] = hi
            }
            Chat.selectAll().where { (userOne eq lo) and (userTwo eq hi) }.single()
                .let { ChatRoom(it[Chat.uuid], it[userOne], it[userTwo]) }
        }
    }

    fun getAllUserChats(userId: Int): List<ChatRoom> {
        return transaction {
            Chat.selectAll()
                .where { (userOne eq userId) or (userTwo eq userId) }
                .map { ChatRoom(it[Chat.uuid], it[userOne], it[userTwo]) }
        }
    }

    fun messages(chatUuid: Uuid): List<ChatMessage> {
        return transaction {
            Message.selectAll().where { Message.chatUuid eq chatUuid }
                .orderBy(Message.createdAt)
                .map(::toMessage)
        }
    }

    fun sendMessage(chatUuid: Uuid, senderId: Int, text: String): ChatMessage {
        require(text.isNotBlank()) { "text must not be blank" }
        return transaction {
            val room = Chat.selectAll().where { Chat.uuid eq chatUuid }.singleOrNull()
                ?: throw IllegalArgumentException("unknown chat")
            require(senderId == room[userOne] || senderId == room[userTwo]) {
                "sender must be a chat participant"
            }
            val now = Clock.System.now()
            val id = Message.insert {
                it[Message.chatUuid] = chatUuid
                it[Message.senderId] = senderId
                it[Message.text] = text
                it[Message.createdAt] = now
            } get Message.id
            ChatMessage(id, chatUuid, senderId, text, now)
        }
    }

    private fun toMessage(row: ResultRow) = ChatMessage(
        row[Message.id],
        row[Message.chatUuid],
        row[Message.senderId],
        row[Message.text],
        row[Message.createdAt]
    )
}

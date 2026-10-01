@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package mr.server.db.entity.chat

import mr.server.db.entity.user.Users
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object Message : Table("message") {
    val id = integer("id").autoIncrement()
    val chatUuid = uuid("chat_uuid").references(Chat.uuid).index()
    val senderId = integer("sender_id").references(Users.id)
    val text = varchar("text", 2000)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

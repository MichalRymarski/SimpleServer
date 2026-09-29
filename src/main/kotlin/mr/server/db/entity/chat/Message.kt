package mr.server.db.entity.chat

import mr.server.db.entity.user.Users
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object Message : Table("message") {
    val id = integer("id").autoIncrement()
    val userOne = integer("user_one")
    val userTwo = integer("user_two")
    val senderId = integer("sender_id").references(Users.id)
    val text = varchar("text", 2000)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        foreignKey(userOne, userTwo, target = Chat.primaryKey)
        index("message_chat_idx", false, userOne, userTwo)
    }
}

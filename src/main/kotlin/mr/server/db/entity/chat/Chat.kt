package mr.server.db.entity.chat

import mr.server.db.entity.user.Users
import org.jetbrains.exposed.v1.core.Table

object Chat : Table("chat") {
    val userOne = integer("user_one").references(Users.id)
    val userTwo = integer("user_two").references(Users.id)

    override val primaryKey = PrimaryKey(userOne, userTwo)
}

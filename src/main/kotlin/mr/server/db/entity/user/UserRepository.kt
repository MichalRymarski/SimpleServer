package mr.server.db.entity.user

import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.Instant

data class User(val id: Int, val name: String, val createdAt: Instant)

object UserRepository {
    fun all(): List<User> = transaction {
        Users.selectAll().map { row ->
            User(row[Users.id], row[Users.name], row[Users.createdAt])
        }
    }

    fun create(name: String): User = transaction {
        val now = Clock.System.now()
        val id = Users.insert {
            it[Users.name] = name
            it[Users.createdAt] = now
        } get Users.id
        User(id, name, now)
    }
}

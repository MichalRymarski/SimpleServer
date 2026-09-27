package mr.server.db.entity.user

import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.Instant

// Domain model: pure Kotlin, no HTTP or JSON concerns.
data class User(val id: Int, val name: String, val createdAt: Instant)

// Thin repository over Exposed transactions. An object, not an injected class:
// Database.connect() registers a global default database, so transaction {}
// needs no handle passed in. If you ever need a second database, use the
// transaction(db) { } overload with an explicit handle instead.
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

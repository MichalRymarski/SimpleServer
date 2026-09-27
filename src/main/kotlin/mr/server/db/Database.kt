package mr.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import mr.server.db.entity.user.Users
import org.http4k.core.Filter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val Log = KotlinLogging.logger {}

// Switching to PostgreSQL later = set DB_URL to "jdbc:postgresql://host:5432/db",
// DB_USER / DB_PASSWORD accordingly, and swap the com.h2database:h2 dependency for
// org.postgresql:postgresql. No code changes needed. (Drop MODE=PostgreSQL then;
// it only exists to keep the H2 dialect close to Postgres.)
data class DbConfig(
    val url: String = System.getenv("DB_URL") ?: "jdbc:h2:./data/simpleserver;MODE=PostgreSQL;AUTO_SERVER=TRUE",
    val user: String = System.getenv("DB_USER") ?: "sa",
    val password: String = System.getenv("DB_PASSWORD") ?: "",
    val driver: String = System.getenv("DB_DRIVER") ?: "org.h2.Driver"
)

private var pool: HikariDataSource? = null
private val lock = Any()

// Idempotent and thread-safe: safe to call from route handlers so every entry
// point (main, hot-reload dev server, tests) gets a connected database.
fun initDatabase(config: DbConfig = DbConfig()) {
    synchronized(lock) {
        if (pool != null) return

        Log.info { "Connecting to database ${config.url}" }
        pool = HikariDataSource(HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            driverClassName = config.driver
            maximumPoolSize = 10
        })
        Database.connect(pool!!)

        transaction {
            // Fine for local dev / H2. Deprecated upstream in favour of real migrations
            // (exposed-migration-jdbc + Flyway) — switch when the schema stabilises.
            @Suppress("DEPRECATION")
            SchemaUtils.createMissingTablesAndColumns(Users)
        }
        Log.info { "Database ready" }
    }
}

fun closeDatabase() {
    pool?.close()
    pool = null
}

// Wrap db routes with this: guarantees a connected database no matter which
// entry point serves them (main, hot-reload dev server, tests). Effectively a
// no-op wherever initDatabase() already ran, thanks to the guard above.
val ensureDatabase: Filter = Filter { next ->
    {
        initDatabase()
        next(it)
    }
}

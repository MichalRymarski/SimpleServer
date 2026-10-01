package mr.server

import mr.server.db.DbConfig
import mr.server.db.closeDatabase
import mr.server.db.initDatabase
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.kotest.shouldHaveStatus
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRouteTest {

    @BeforeAll
    fun setup() {
        initDatabase(
            DbConfig(
                url = "jdbc:h2:mem:dbroutetest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                user = "sa",
                password = ""
            )
        )
    }

    @AfterAll
    fun teardown() {
        closeDatabase()
    }

    @BeforeEach
    fun clean() {
        transaction {
            exec("DELETE FROM users")
        }
    }

    @Test
    fun `empty user list`() {
        val response = app(Request(GET, "/users"))

        response shouldHaveStatus OK
        check(response.bodyString() == "[]") { "unexpected body: ${response.bodyString()}" }
    }

    @Test
    fun `create and list users`() {
        val created = app(
            Request(POST, "/users")
                .header("Content-Type", "application/json")
                .body("""{"name":"jim"}""")
        )

        created shouldHaveStatus CREATED
        check(created.bodyString().contains("jim")) { "unexpected body: ${created.bodyString()}" }

        val listed = app(Request(GET, "/users"))
        listed shouldHaveStatus OK
        check(listed.bodyString().contains("jim")) { "unexpected body: ${listed.bodyString()}" }
    }
}

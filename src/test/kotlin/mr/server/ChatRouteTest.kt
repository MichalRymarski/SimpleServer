package mr.server

import mr.server.db.DbConfig
import mr.server.db.closeDatabase
import mr.server.db.entity.user.UserRepository
import mr.server.db.initDatabase
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.kotest.shouldHaveStatus
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatRouteTest {

    @BeforeAll
    fun setup() {
        initDatabase(
            DbConfig(
                url = "jdbc:h2:mem:chatroutetest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
            exec("DELETE FROM message")
            exec("DELETE FROM chat")
            exec("DELETE FROM users")
        }
    }

    @Test
    fun `create chat and send messages`() {
        val jim = UserRepository.create("jim")
        val bob = UserRepository.create("bob")

        val chat = app(
            Request(POST, "/db/chats")
                .header("Content-Type", "application/json")
                .body("""{"userOne":${bob.id},"userTwo":${jim.id}}""")
        )
        chat shouldHaveStatus OK
        // pair is order-normalized to (min, max)
        check(chat.bodyString().contains(""""userOne":${jim.id},"userTwo":${bob.id}""")) {
            "unexpected body: ${chat.bodyString()}"
        }

        val sent = app(
            Request(POST, "/db/chats/messages")
                .header("Content-Type", "application/json")
                .body("""{"userOne":${jim.id},"userTwo":${bob.id},"senderId":${jim.id},"text":"hi bob"}""")
        )
        sent shouldHaveStatus CREATED
        check(sent.bodyString().contains("hi bob")) { "unexpected body: ${sent.bodyString()}" }

        // reversed pair order reads the same room
        val listed = app(Request(GET, "/db/chats/${bob.id}/${jim.id}/messages"))
        listed shouldHaveStatus OK
        check(listed.bodyString().contains("hi bob")) { "unexpected body: ${listed.bodyString()}" }
    }

    @Test
    fun `sender outside the chat is a 400`() {
        val jim = UserRepository.create("jim")
        val bob = UserRepository.create("bob")
        val eve = UserRepository.create("eve")

        app(
            Request(POST, "/db/chats/messages")
                .header("Content-Type", "application/json")
                .body("""{"userOne":${jim.id},"userTwo":${bob.id},"senderId":${eve.id},"text":"snoop"}""")
        ) shouldHaveStatus BAD_REQUEST
    }

    @Test
    fun `chat with yourself is a 400`() {
        val jim = UserRepository.create("jim")

        app(
            Request(POST, "/db/chats")
                .header("Content-Type", "application/json")
                .body("""{"userOne":${jim.id},"userTwo":${jim.id}}""")
        ) shouldHaveStatus BAD_REQUEST
    }
}

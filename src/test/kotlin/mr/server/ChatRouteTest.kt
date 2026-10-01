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

    private fun chatUuidOf(body: String): String {
        val match = Regex(""""uuid":"([0-9a-fA-F-]+)"""").find(body)
        check(match != null) { "no uuid in body: $body" }
        return match.groupValues[1]
    }

    @Test
    fun `create chat, send and list by uuid`() {
        val jim = UserRepository.create("jim")
        val bob = UserRepository.create("bob")

        val chat = app(
            Request(POST, "/chat")
                .header("Content-Type", "application/json")
                .body("""{"userOne":${jim.id},"userTwo":${bob.id}}""")
        )
        chat shouldHaveStatus OK
        val uuid = chatUuidOf(chat.bodyString())

        // same pair, reversed order resolves to the same room
        val again = app(
            Request(POST, "/chat")
                .header("Content-Type", "application/json")
                .body("""{"userOne":${bob.id},"userTwo":${jim.id}}""")
        )
        again shouldHaveStatus OK
        check(chatUuidOf(again.bodyString()) == uuid) { "pair order forked rooms" }

        val sent = app(
            Request(POST, "/chat/$uuid/messages")
                .header("Content-Type", "application/json")
                .body("""{"senderId":${jim.id},"text":"hi bob"}""")
        )
        sent shouldHaveStatus CREATED
        check(sent.bodyString().contains("hi bob")) { "unexpected body: ${sent.bodyString()}" }

        val listed = app(Request(GET, "/chat/$uuid/messages"))
        listed shouldHaveStatus OK
        check(listed.bodyString().contains("hi bob")) { "unexpected body: ${listed.bodyString()}" }
    }

    @Test
    fun `chat with yourself is a 400`() {
        val jim = UserRepository.create("jim")

        app(
            Request(POST, "/chat")
                .header("Content-Type", "application/json")
                .body("""{"userOne":${jim.id},"userTwo":${jim.id}}""")
        ) shouldHaveStatus BAD_REQUEST
    }

    @Test
    fun `outsider sender is a 400`() {
        val jim = UserRepository.create("jim")
        val bob = UserRepository.create("bob")
        val eve = UserRepository.create("eve")

        val chat = app(
            Request(POST, "/chat")
                .header("Content-Type", "application/json")
                .body("""{"userOne":${jim.id},"userTwo":${bob.id}}""")
        )
        val uuid = chatUuidOf(chat.bodyString())

        app(
            Request(POST, "/chat/$uuid/messages")
                .header("Content-Type", "application/json")
                .body("""{"senderId":${eve.id},"text":"snoop"}""")
        ) shouldHaveStatus BAD_REQUEST
    }

    @Test
    fun `list chats for user`() {
        val jim = UserRepository.create("jim")
        val bob = UserRepository.create("bob")

        val chat = app(
            Request(POST, "/chat")
                .header("Content-Type", "application/json")
                .body("""{"userOne":${jim.id},"userTwo":${bob.id}}""")
        )
        val uuid = chatUuidOf(chat.bodyString())

        val listed = app(Request(GET, "/chat?id=${jim.id}"))
        listed shouldHaveStatus OK
        check(listed.bodyString().contains(uuid)) { "unexpected body: ${listed.bodyString()}" }
    }
}

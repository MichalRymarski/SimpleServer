package mr.server

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.kotest.shouldHaveBody
import org.http4k.kotest.shouldHaveStatus
import org.junit.jupiter.api.Test

class JteTemplateTest {

    @Test
    fun `renders jte template`() {
        val response = app(Request(GET, "/templates/jte"))

        println(response)
        response shouldHaveStatus OK
        response shouldHaveBody "<html><span>Hello there!</span></html>\n"
    }

    @Test
    fun `renders test template with null param`() {
        val response = app(Request(GET, "/templates/jte/test"))

        response shouldHaveStatus OK
        response shouldHaveBody "<html>\n<body><h1>Test template</h1>\n<p>one: 1</p><p>two is null</p></body>\n</html>\n"
    }
}

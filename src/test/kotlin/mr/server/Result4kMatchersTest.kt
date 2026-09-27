package mr.server

import dev.forkhandles.result4k.kotest.shouldBeFailure
import dev.forkhandles.result4k.kotest.shouldBeSuccess
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.Failure
import org.http4k.lens.Query
import org.http4k.lens.asResult
import org.http4k.lens.int
import org.junit.jupiter.api.Test

class Result4kMatchersTest {

    private val port = Query.int().required("port")

    @Test
    fun `shouldBeSuccess unwraps a successful lens extraction`() {
        port.asResult()(Request(GET, "/?port=9000")) shouldBeSuccess 9000
    }

    @Test
    fun `shouldBeFailure captures a missing lens value`() {
        val failure = port.asResult()(Request(GET, "/")).shouldBeFailure()
        check(failure.failures.single().meta.name == "port")
        check(failure.overall() == Failure.Type.Missing)
    }
}

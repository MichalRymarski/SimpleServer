package mr.server.models

import org.http4k.template.ViewModel

data class TestViewModel(val one: Number, val two: Number?) : ViewModel

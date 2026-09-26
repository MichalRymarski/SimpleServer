package mr.server.formats

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import java.nio.file.Paths

// JTE templates precompiled at build time (precompileJte) into jte-classes/.
// No runtime compilation, so this works on any classpath - including hot reload.
//
// The engine is built per render, parented on the model's own classloader: under
// hot reload the model class exists in two loaders (app vs reload copy) and the
// template must link against the same copy that is passed in. Engine creation
// itself is cheap (no compilation, just a loader + empty cache).
val precompiledJteRenderer: TemplateRenderer = { viewModel: ViewModel ->
    val engine = TemplateEngine.createPrecompiled(
        Paths.get("jte-classes"),
        ContentType.Html,
        viewModel.javaClass.classLoader
    )
    StringOutput()
        .also { engine.render(viewModel.template() + ".kte", viewModel, it) }
        .toString()
}

package ratpack.kotlin

import ratpack.file.FileHandlerSpec
import ratpack.handling.Chain

class KChain (private val delegate: Chain) : Chain by delegate {
    fun files(cb: FileHandlerSpec.() -> Unit) =
        delegate.files { it.(cb)() }
    fun fileSystem(path: String = "", cb: KChain.() -> Unit) =
        delegate.fileSystem (path) { KChain(it).(cb)() }
    fun prefix(path: String = "", cb: KChain.() -> Unit) =
        delegate.prefix (path) { KChain(it).(cb)() }

    fun all(cb: KContext.() -> Unit) =
        delegate.all { KContext(it).(cb)() }
    fun path(path: String = "", cb: KContext.() -> Unit) =
        delegate.path (path) { KContext(it).(cb)() }

    @Suppress("ReplaceGetOrSet")
    fun get(path: String = "", cb: KContext.() -> Unit) =
        delegate.get (path) { KContext(it).(cb)() }

    fun put(path: String = "", cb: KContext.() -> Unit) =
        delegate.put (path) { KContext(it).(cb)() }

    fun post(path: String = "", cb: KContext.() -> Unit) =
        delegate.post (path) { KContext(it).(cb)() }

    fun delete(path: String = "", cb: KContext.() -> Unit) =
        delegate.delete (path) { KContext(it).(cb)() }

    fun options(path: String = "", cb: KContext.() -> Unit) =
        delegate.options (path) { KContext(it).(cb)() }

    fun patch(path: String = "", cb: KContext.() -> Unit) =
        delegate.patch (path) { KContext(it).(cb)() }
}

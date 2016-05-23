package ratpack.kotlin

import ratpack.registry.Registry
import ratpack.registry.RegistrySpec
import ratpack.server.RatpackServerSpec

class KServerSpec(private val delegate: RatpackServerSpec) : RatpackServerSpec by delegate {
    fun serverConfig(cb: KServerConfigBuilder.() -> Unit) =
        delegate.serverConfig { KServerConfigBuilder(it).(cb)() }
    fun registry(cb: RegistrySpec.() -> Unit) = delegate.registry (Registry.of(cb))
    fun handlers(cb: KChain.() -> Unit) = delegate.handlers { KChain(it).(cb)() }
}

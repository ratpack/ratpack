package ratpack.rx2.internal

import com.google.inject.Injector
import com.google.inject.Module
import ratpack.groovy.Groovy
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.internal.ClosureUtil
import ratpack.guice.BindingsSpec
import ratpack.guice.Guice
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import ratpack.server.ServerConfigBuilder
import ratpack.test.embed.EmbeddedApp

abstract class RatpackGroovyDslSpec extends EmbeddedBaseDirRatpackSpec {

  protected final List<Module> modules = []
  protected Closure<?> _handlers = ClosureUtil.noop()
  protected Closure<?> _bindings = ClosureUtil.noop()
  protected Closure<?> _serverConfig = ClosureUtil.noop()
  protected Injector parentInjector

  @Delegate
  final EmbeddedApp application = createApplication()

  protected EmbeddedApp createApplication() {
    fromServer {
      RatpackServer.of {
        it.serverConfig(serverConfigBuilder())

        def bindingsAction = { s ->
          s.with(_bindings)
          modules.each { s.module(it) }
        }

        it.registry(parentInjector ? Guice.registry(parentInjector, bindingsAction) : Guice.registry(bindingsAction))
        it.handler { Groovy.chain(it, _handlers) }
      }
    }
  }

  protected ServerConfigBuilder serverConfigBuilder() {
    def serverConfig = ServerConfig.builder()
    if (this.baseDir) {
      serverConfig.baseDir(this.baseDir.root)
    }
    serverConfig.port(0)
    serverConfig.with(_serverConfig)
    serverConfig
  }

  void handlers(@DelegatesTo(value = GroovyChain, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    _handlers = configurer
  }

  void bindings(@DelegatesTo(value = BindingsSpec, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    _bindings = configurer
  }

  void serverConfig(@DelegatesTo(value = ServerConfigBuilder, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    _serverConfig = configurer
  }

}

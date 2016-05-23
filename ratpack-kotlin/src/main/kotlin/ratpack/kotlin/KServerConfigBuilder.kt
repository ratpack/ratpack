package ratpack.kotlin

import ratpack.server.ServerConfigBuilder
import java.net.InetAddress
import java.nio.file.Path

class KServerConfigBuilder(private val delegate: ServerConfigBuilder) :
    ServerConfigBuilder by delegate {

    var port: Int
        get() = throw IllegalStateException("Port is a write only property")
        set (value) { delegate.port(value) }

    var address: InetAddress
        get() = throw IllegalStateException("Address is a write only property")
        set (value) { delegate.address(value) }

    var baseDir: Path
        get() = throw IllegalStateException("BaseDir is a write only property")
        set (value) { delegate.baseDir(value) }
}

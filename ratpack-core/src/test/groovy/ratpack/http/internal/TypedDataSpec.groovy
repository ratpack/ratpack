package ratpack.http.internal

import io.netty.buffer.Unpooled
import spock.lang.Specification


class TypedDataSpec extends Specification {

  def "can return bytes from array-backed Netty buffers"() {
    given:
    def buffer = Unpooled.buffer(2000)
    buffer.writeBytes("foo".bytes)
    def typedData = new ByteBufBackedTypedData(buffer, DefaultMediaType.get("text/plain"))

    expect:
    typedData.bytes == "foo".bytes
  }
}

import groovy.text.GStringTemplateEngine
import groovy.text.SimpleTemplateEngine

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean



class Template {
  String id
  Closure done
  String text = "processing"
  def random = new Random()

  Template(String id, Closure done) {
    this.id = id
    this.done = done

    Thread.start {
      sleep (random.nextInt(10))
      text = id
      done(this)
    }
  }

  @Override
  String toString() {
    text
  }
}
class Renderer {
  Map<String, Template> inners = [:]
  Map<String, Template> processing = new ConcurrentHashMap<String, Template>()
  AtomicBoolean doneFired = new AtomicBoolean()

  Closure done

  def render(String id) {
    if (inners.containsKey(id)) {
      println "returning template: $id"
      inners[id]
    } else {
      println "creating template: $id"
      def template = new Template(id, { innerFinished(it) })
      inners[id] = template
      processing[id] = template
      template
    }
  }

  protected innerFinished(Template inner) {
    processing.remove(inner.id)
    if (processing.isEmpty() && doneFired.compareAndSet(false, true)) {
      done()
    }
  }
}

def renderer = new Renderer()

def engine = new SimpleTemplateEngine()
engine.verbose = true
def template = engine.createTemplate('${(0..1000).collect { render(it.toString()) }}')
final writable = template.make(render: { renderer.render(it) })
def latch = new CountDownLatch(1)
renderer.done = {
  def writer = new StringWriter()
  writable.writeTo(writer)
  println writer.toString()
  latch.countDown()
}

def writer1 = new StringWriter()
writable.writeTo(writer1)
println writer1


latch.await()
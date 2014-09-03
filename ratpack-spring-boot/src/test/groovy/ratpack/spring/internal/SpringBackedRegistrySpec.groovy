package ratpack.spring.internal

import com.google.common.base.Predicates
import com.google.common.reflect.TypeToken
import org.springframework.context.support.StaticApplicationContext
import ratpack.func.Action
import ratpack.registry.NotInRegistryException
import ratpack.registry.Registry
import spock.lang.Specification

class SpringBackedRegistrySpec extends Specification {
  def appContext = new StaticApplicationContext()
  def r = new SpringBackedRegistry(appContext)
  def beanFactory = appContext.getBeanFactory()

  def "empty registry"() {
    expect:
    r.maybeGet(String) == null

    when:
    r.get(String)

    then:
    thrown NotInRegistryException
  }

  def "get and getAll should support implemented interfaces besides actual class"() {
    when:
    beanFactory.registerSingleton("foo", "foo")

    then:
    r.get(String) == "foo"
    r.get(CharSequence) == "foo"
    r.getAll(String).toList() == ["foo"]
    r.getAll(CharSequence).toList() == ["foo"]
  }

  def "search empty registry with always false predicate"() {
    expect:
    r.first(TypeToken.of(Object), Predicates.alwaysFalse()) == null
    r.all(TypeToken.of(Object), Predicates.alwaysFalse()) as List == []
  }

  def "search with one item"() {
    given:
    TypeToken type = TypeToken.of(String)
    TypeToken other = TypeToken.of(Number)
    def value = "Something"

    beanFactory.registerSingleton("value", value)

    expect:
    r.first(type, Predicates.alwaysTrue()) == value
    r.first(type, Predicates.alwaysFalse()) == null
    r.first(other, Predicates.alwaysTrue()) == null
    r.first(other, Predicates.alwaysFalse()) == null

    r.all(type, Predicates.alwaysTrue()) as List == [value]
    r.all(type, Predicates.alwaysFalse()) as List == []
    r.all(other, Predicates.alwaysTrue()) as List == []
    r.all(other, Predicates.alwaysFalse()) as List == []
  }

  def "search with multiple items"() {
    given:
    TypeToken string = TypeToken.of(String)
    TypeToken number = TypeToken.of(Number)
    def a = "A"
    def b = "B"
    def c = 42
    def d = 16
    beanFactory.registerSingleton("a", a)
    beanFactory.registerSingleton("b", b)
    beanFactory.registerSingleton("c", c)
    beanFactory.registerSingleton("d", d)

    expect:
    r.first(string, Predicates.alwaysTrue()) == a
    r.first(string, { s -> s.startsWith('B') }) == b
    r.first(number, Predicates.alwaysTrue()) == c
    r.first(number, { n -> n < 20 }) == d

    r.all(string, Predicates.alwaysTrue()) as List == [a, b]
    r.all(string, { s -> s.startsWith('B') }) as List == [b]
    r.all(number, { n -> n < 50 })  as List == [c, d]
    r.all(number, Predicates.alwaysFalse()) as List == []
  }

  def "each with action"() {
    given:
    Action action = Mock()
    def sameType = TypeToken.of(String)
    def differentType = TypeToken.of(Number)
    def value = "Something"
    beanFactory.registerSingleton("value", value)

    when:
    r.each(sameType, Predicates.alwaysTrue(), action)

    then:
    1 * action.execute(value)

    when:
    r.each(sameType, Predicates.alwaysFalse(), action)

    then:
    0 * action.execute(_)

    when:
    r.each(differentType, Predicates.alwaysTrue(), action)
    r.each(differentType, Predicates.alwaysFalse(), action)

    then:
    0 * action.execute(_)
  }

  def "find first"() {
    given:
    def sameType = TypeToken.of(String)
    def differentType = TypeToken.of(Number)
    def value = "Something"
    beanFactory.registerSingleton("value", value)
    expect:
    r.first(sameType, Predicates.alwaysTrue()) == value
    r.first(sameType, Predicates.alwaysFalse()) == null
    r.first(differentType, Predicates.alwaysTrue()) == null
    r.first(differentType, Predicates.alwaysFalse()) == null
  }

  def "find all"() {
    given:
    def sameType = TypeToken.of(String)
    def differentType = TypeToken.of(Number)
    def value = "Something"
    beanFactory.registerSingleton("value", value)
    expect:
    r.all(sameType, Predicates.alwaysTrue()).toList() == [value]
    r.all(sameType, Predicates.alwaysFalse()).toList() == []
    r.all(differentType, Predicates.alwaysTrue()).toList() == []
    r.all(differentType, Predicates.alwaysFalse()).toList() == []
  }
}
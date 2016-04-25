package ratpack.exec

import ratpack.test.exec.ExecHarness
import spock.lang.Specification

/**
 *
 */
class PromiseForkEachSpec extends  Specification {

    ExecHarness exec = ExecHarness.harness()

    def "fork promises with successful result values"() {
        given:
        Set<Promise<String>> promises = (1..9).collect { Promise.value("Promise $it") }.toSet()

        when:
        ExecResult<Set<Result<String>>> result = exec.yieldSingle { e ->
            Promise.forkEach(promises)
        }

        then:
        result.value.size() == promises.size()
        result.value.collect { r ->
            assert r.success
            assert r.value != null
            assert !r.error
            assert r.throwable == null
            r.value
        }.sort().eachWithIndex { s, i ->
            assert s == "Promise ${i + 1}".toString()
        }
    }

    def "fork promises with error result values"() {
        given:
        Set<Promise<String>> promises = (1..9).collect { Promise.error(new ExecutionException("Promise $it")) }.toSet()

        when:
        ExecResult<Set<Result<String>>> result = exec.yieldSingle { e ->
            Promise.forkEach(promises)
        }

        then:
        result.value.size() == promises.size()
        result.value.collect { r ->
            assert !r.success
            assert r.value == null
            assert r.error
            assert r.throwable != null
            r.throwable.message
        }.sort().eachWithIndex { s, i ->
            assert s == "Promise ${i + 1}".toString()
        }
    }

    def "fork promises with execution exception"() {
        given:
        Set<Promise<String>> promises = (1..9).collect {
            Promise.async { d ->
                throw new ExecutionException("Promise $it")
            }
        }.toSet()

        when:
        ExecResult<Set<Result<String>>> result = exec.yieldSingle { e ->
            Promise.forkEach(promises)
        }

        then:
        result.value.size() == promises.size()
        result.value.collect { r ->
            assert !r.success
            assert r.value == null
            assert r.error
            assert r.throwable != null
            r.throwable.message
        }.sort().eachWithIndex { s, i ->
            assert s == "Promise ${i + 1}".toString()
        }
    }

}

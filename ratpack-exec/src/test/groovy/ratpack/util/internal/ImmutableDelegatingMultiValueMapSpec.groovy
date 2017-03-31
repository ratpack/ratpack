/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.util.internal

import spock.lang.Specification

class ImmutableDelegatingMultiValueMapSpec extends Specification {

  def delegate = Mock(Map)
  def map = new ImmutableDelegatingMultiValueMap<String, String>(delegate)

  void 'size'() {
    when:
    def size = map.size()

    then:
    1 * delegate.size() >> 3
    size == 3
  }

  void 'isEmpty - #isEmpty'() {
    when:
    def isEmptyResult = map.isEmpty()

    then:
    1 * delegate.isEmpty() >> isEmpty
    isEmptyResult == isEmpty

    where:
    isEmpty << [true, false]
  }

  void 'containsKey - #containsKey'() {
    when:
    def containsKeyResult = map.containsKey(key)

    then:
    1 * delegate.containsKey(key) >> containsKey
    containsKeyResult == containsKey

    where:
    key         | containsKey
    'key'       | true
    'other key' | false
  }

  void 'containsValue - #containsValue'() {
    when:
    def containsValueResult = map.containsValue(value)

    then:
    1 * delegate.values() >> [[], ['single value'], ['some value', 'value']]
    containsValueResult == containsValue

    where:
    value         | containsValue
    'value'       | true
    'other value' | false
  }

  void 'get - #key'() {
    when:
    def getResult = map.get(key)

    then:
    1 * delegate.get(key) >> delegateGet
    getResult == get

    where:
    key                 | delegateGet                     | get
    'no such key entry' | null                            | null
    'empty list'        | []                              | null
    'single value'      | ['value']                       | 'value'
    'multiple values'   | ['first value', 'second value'] | 'first value'
  }

  void 'getAll for key - #key'() {
    when:
    def getAllResult = map.getAll(key)

    then:
    1 * delegate.get(key) >> delegateGet
    getAllResult == getAll

    where:
    key               | delegateGet                     | getAll
    'empty list'      | []                              | []
    'single value'    | ['value']                       | ['value']
    'multiple values' | ['first value', 'second value'] | ['first value', 'second value']
  }

  void 'get all'() {
    expect:
    map.getAll() == delegate
  }

  void 'put'() {
    when:
    map.put('key', 'value')

    then:
    UnsupportedOperationException e = thrown()
    e.message == 'This implementation is immutable'
  }

  void 'clear'() {
    when:
    map.clear()

    then:
    UnsupportedOperationException e = thrown()
    e.message == 'This implementation is immutable'
  }

  void 'key set'() {
    given:
    def keySet = ['a', 'b', 'c'] as Set

    when:
    def keySetResult = map.keySet()

    then:
    1 * delegate.keySet() >> keySet
    keySetResult == keySet
  }

  void 'values'() {
    when:
    def valuesResult = map.values()

    then:
    1 * delegate.values() >> [['a'], [], ['b', 'c']]
    valuesResult == ['a', 'b', 'c']
  }

  void 'remove'() {
    when:
    map.remove(null)

    then:
    UnsupportedOperationException e = thrown()
    e.message == 'This implementation is immutable'
  }

  void 'put all'() {
    when:
    map.putAll([:])

    then:
    UnsupportedOperationException e = thrown()
    e.message == 'This implementation is immutable'
  }

  void 'entry set'() {
    when:
    def entrySetResult = map.entrySet()

    then:
    1 * delegate.entrySet() >> [
      new AbstractMap.SimpleImmutableEntry<String, List<String>>('empty', []),
      new AbstractMap.SimpleImmutableEntry<String, List<String>>('value', ['value']),
      new AbstractMap.SimpleImmutableEntry<String, List<String>>('multi value', ['first value', 'other multi value'])
    ]
    entrySetResult == [
      new AbstractMap.SimpleImmutableEntry<String, String>('value', 'value'),
      new AbstractMap.SimpleImmutableEntry<String, String>('multi value', 'first value')
    ] as Set
  }

  void 'to string'() {
    expect:
    new ImmutableDelegatingMultiValueMap(wrapped).toString() == wrapped.toString()

    where:
    wrapped << [[a: ['b'], c: ['d', 'e'], f: []], [:]]
  }

}

/*
 * Copyright 2014 the original author or authors.
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

package ratpack.hikari

import groovy.sql.Sql
import ratpack.groovy.sql.SqlModule
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

public class HikariModuleSpec extends RatpackGroovyDslSpec {

  @Unroll
  def "can use db when module #scenario"() {
    when:
    bindings {
      add new SqlModule(), new HikariModule(*moduleConstructorArgs)

      init { Sql sql ->
        sql.execute("create table if not exists val(ID INT PRIMARY KEY, val VARCHAR(255));")
      }
    }

    launchConfig {
      other(otherSystemProperties)
    }

    handlers { Sql sql ->
      get('schema') {
        def schemas = sql.rows('show schemas')
        render schemas.first()[0]
      }

      get("get/:id") {
        def row = sql.firstRow("select val from val where id = ${pathTokens.asInt("id")}")
        render row.val
      }

      post("set/:id/:val") {
        sql.executeInsert("merge into val (id, val) key(id) values (${pathTokens.asInt("id")}, $pathTokens.val)")
        response.send()
      }
    }

    then:
    post('set/0/foo')
    getText('get/0') == "foo"
    getText('schema') == 'DEV'

    where:
    scenario                                       | moduleConstructorArgs                                                                           | otherSystemProperties
    'is configured using constructor args'         | [[URL: "jdbc:h2:mem:dev;INIT=CREATE SCHEMA IF NOT EXISTS DEV"], "org.h2.jdbcx.JdbcDataSource"]  | [:]
    'is configured using system properties'        | []                                                                                              | ["hikari.dataSourceClassName": "org.h2.jdbcx.JdbcDataSource", "hikari.dataSourceProperties.URL": "jdbc:h2:mem:dev;INIT=CREATE SCHEMA IF NOT EXISTS DEV"]
    'config is overridden using system properties' | [[URL: "jdbc:h2:mem:dev;INIT=CREATE SCHEMA IF NOT EXISTS TEST"], "org.h2.jdbcx.JdbcDataSource"] | ["hikari.dataSourceProperties.URL": "jdbc:h2:mem:dev;INIT=CREATE SCHEMA IF NOT EXISTS DEV"]
  }
}

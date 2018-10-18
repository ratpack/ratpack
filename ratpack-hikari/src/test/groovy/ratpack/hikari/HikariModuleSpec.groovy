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
import ratpack.server.Service
import ratpack.server.StartEvent
import ratpack.test.internal.RatpackGroovyDslSpec

class HikariModuleSpec extends RatpackGroovyDslSpec {

  def "can use db"() {
    when:
    bindings {
      module SqlModule
      module HikariModule, {
        it.addDataSourceProperty("URL", "jdbc:h2:mem:dev;INIT=CREATE SCHEMA IF NOT EXISTS DEV")
        it.dataSourceClassName = "org.h2.jdbcx.JdbcDataSource"
      }

      bindInstance Service, new Service() {
        @Override
        void onStart(StartEvent event) {
          event.registry.get(Sql).execute("create table if not exists val(ID INT PRIMARY KEY, val VARCHAR(255));")
        }
      }
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
  }
}

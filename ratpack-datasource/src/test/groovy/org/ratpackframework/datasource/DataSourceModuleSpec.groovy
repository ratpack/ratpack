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

package org.ratpackframework.datasource

import groovy.sql.Sql
import org.ratpackframework.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

import java.sql.Connection

/**
 * @author Andres Almiray
 * @author Lukasz Pielak
 * @author Tom Bujok
 * @author Dmitry Vyazelenko
 */
@Unroll
class DataSourceModuleSpec extends RatpackGroovyDslSpec {
  void 'Datasource #dataSourceName should have URL #dataSourceURL'() {
    when:
    app {
      modules {
        register new DataSourceModule()
      }
      handlers {
        get { DataSourceProvider dataSourceProvider ->
          Connection connection = null
          try {
            connection = dataSourceProvider.fetchDataSource(dataSourceName).connection
            response.send(connection.metaData.getURL().toString())
          } finally {
            connection?.close()
          }
        }
      }
    }

    then:
    text == dataSourceURL

    where:
    dataSourceName || dataSourceURL
    null           || 'jdbc:h2:mem:default'
    'default'      || 'jdbc:h2:mem:default'
    'production'   || 'jdbc:h2:mem:production'
  }

  void 'Schema is created when configuration says it should'() {
    when:
    app {
      modules {
        register new DataSourceModule()
      }
      handlers {
        get { DataSourceProvider dataSourceProvider ->
          Sql sql = new Sql(dataSourceProvider.fetchDataSource('development'))
          def people = sql.dataSet('people')
          people.add(id: 1, name: 'Joe', lastname: 'Doe')
          String output = sql.firstRow('SELECT * FROM people WHERE id = ?', [1]).entrySet().toString()
          response.send(output)
        }
      }
    }

    then:
    text == '[ID=1, NAME=Joe, LASTNAME=Doe]'
  }
}
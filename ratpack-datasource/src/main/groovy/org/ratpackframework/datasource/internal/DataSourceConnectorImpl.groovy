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
package org.ratpackframework.datasource.internal

import groovy.sql.Sql
import org.apache.commons.dbcp.ConnectionFactory
import org.apache.commons.dbcp.DriverManagerConnectionFactory
import org.apache.commons.dbcp.PoolableConnectionFactory
import org.apache.commons.dbcp.PoolingDataSource
import org.apache.commons.pool.impl.GenericObjectPool
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation
import org.ratpackframework.datasource.DataSourceConnector

import javax.sql.DataSource
import java.util.logging.Logger

/**
 * @author Andres Almiray
 * @author Lukasz Pielak
 * @author Tom Bujok
 * @author Dmitry Vyazelenko
 */
class DataSourceConnectorImpl implements DataSourceConnector {
  private final Logger logger = Logger.getLogger(getClass().getName());
  private static final String DEFAULT = 'default'
  private ConfigObject dataSourceConfig

  ConfigObject createConfig() {
    if (!dataSourceConfig) {
      try {
        // try datasource.groovy first
        GroovyClassLoader gcl = new GroovyClassLoader(this.class.classLoader)
        Class datasourceClass = gcl.loadClass('datasource')
        dataSourceConfig = new ConfigSlurper().parse(datasourceClass)
      } catch (ClassNotFoundException cnfe) {
        // read datasource.properties
        Properties props = new Properties()
        // let potential IOExceptions bubble up
        props.load(this.class.classLoader.getResourceAsStream('datasource.properties'))
        dataSourceConfig = new ConfigSlurper().parse(props)
      }
    }
    dataSourceConfig
  }

  DataSource connect(ConfigObject config, String dataSourceName = DEFAULT) {
    config = narrowConfig(config, dataSourceName)
    DataSource dataSource = createDataSource(config)
    if (config.schema == 'create') {
      createSchema(config, dataSourceName, dataSource)
    }
    dataSource
  }

  private static ConfigObject narrowConfig(ConfigObject config, String dataSourceName) {
    if (config.containsKey('dataSource') && dataSourceName == DEFAULT) {
      return config.dataSource
    } else if (config.containsKey('dataSources')) {
      return config.dataSources[dataSourceName]
    }
    return config
  }

  private static DataSource createDataSource(ConfigObject config) {
    Class.forName(config.driverClassName.toString())
    GenericObjectPool connectionPool = new GenericObjectPool(null)
    if (config.pool) {
      // TODO allow additional pool settings
      if (config.pool.maxWait != null) {
        connectionPool.maxWait = config.pool.maxWait as int
      }
      if (config.pool.maxIdle != null) {
        connectionPool.maxIdle = config.pool.maxIdle as int
      }
      if (config.pool.maxActive != null) {
        connectionPool.maxActive = config.pool.maxActive as int
      }
    }

    String url = config.url.toString()
    String username = config.username.toString()
    String password = config.password.toString()
    ConnectionFactory connectionFactory
    if (username) {
      connectionFactory = new DriverManagerConnectionFactory(url, username, password)
    } else {
      connectionFactory = new DriverManagerConnectionFactory(url, null)
    }
    new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true)
    new PoolingDataSource(connectionPool)
  }


  private void createSchema(ConfigObject config, String dataSourceName, DataSource dataSource) {

    URL ddl = null
    for (String schemaName : [dataSourceName + '-schema.ddl', 'schema.ddl']) {
      ddl = this.class.classLoader.getResource(schemaName)
      if (!ddl) {
        logger.warning("DataSource[${dataSourceName}].schema was set to 'create' but ${schemaName} was not found in classpath")
      } else {
        break
      }
    }
    if (!ddl) {
      logger.severe("DataSource[${dataSourceName}].schema was set to 'create' but no suitable schema was found in classpath")
      return
    }

    boolean tokenizeddl = castToBoolean(config.tokenizeddl ?: false)
    Sql sql = new Sql(dataSource)
    if (!tokenizeddl) {
      sql.execute(ddl.text)
    } else {
      ddl.text.split(';').each { stmnt ->
        if (stmnt?.trim()) {
          sql.execute(stmnt + ';')
        }
      }
    }
  }

  private static boolean castToBoolean(Object value) {
    if (value instanceof CharSequence) {
      return 'true'.equalsIgnoreCase(value.toString())
    }
    return DefaultTypeTransformation.castToBoolean(value)
  }
}
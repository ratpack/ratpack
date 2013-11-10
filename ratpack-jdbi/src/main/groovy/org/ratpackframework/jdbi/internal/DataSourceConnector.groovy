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
package org.ratpackframework.jdbi.internal

import org.apache.commons.dbcp.ConnectionFactory
import org.apache.commons.dbcp.DriverManagerConnectionFactory
import org.apache.commons.dbcp.PoolableConnectionFactory
import org.apache.commons.dbcp.PoolingDataSource
import org.apache.commons.pool.ObjectPool
import org.apache.commons.pool.impl.GenericObjectPool

import javax.sql.DataSource

/**
 * @author Andres Almiray
 * @author Lukasz Pielak
 * @author Tom Bujok
 * @author Dmitry Vyazelenko
 */
@Singleton
class DataSourceConnector {

  private static final String DEFAULT = 'default'
  private ConfigObject dataSourceConfig;

  ConfigObject createConfig() {
    if (!dataSourceConfig) {
      GroovyClassLoader gcl = new GroovyClassLoader(this.class.classLoader)
      Class datasourceClass = gcl.loadClass('datasource')
      dataSourceConfig = new ConfigSlurper().parse(datasourceClass)
    }
    dataSourceConfig
  }

  private ConfigObject narrowConfig(ConfigObject config, String dataSourceName) {
    if (config.containsKey('dataSource') && dataSourceName == DEFAULT) {
      return config.dataSource
    } else if (config.containsKey('dataSources')) {
      return config.dataSources[dataSourceName]
    }
    return config
  }

  DataSource connect(ConfigObject config, String dataSourceName = DEFAULT) {
    if (DataSourceHolder.instance.isDataSourceConnected(dataSourceName)) {
      return DataSourceHolder.instance.getDataSource(dataSourceName)
    }

    config = narrowConfig(config, dataSourceName)
    DataSource ds = createDataSource(config)
    DataSourceHolder.instance.setDataSource(dataSourceName, ds)
    ds
  }

  void disconnect(String dataSourceName = DEFAULT) {
    if (DataSourceHolder.instance.isDataSourceConnected(dataSourceName)) {
      DataSourceHolder.instance.disconnectDataSource(dataSourceName)
    }
  }

  private DataSource createDataSource(ConfigObject config) {
    Class.forName(config.driverClassName.toString())
    ObjectPool connectionPool = new GenericObjectPool(null)
    if (config.pool) {
      if (config.pool.maxWait != null) {
        connectionPool.maxWait = config.pool.maxWait
      }
      if (config.pool.maxIdle != null) {
        connectionPool.maxIdle = config.pool.maxIdle
      }
      if (config.pool.maxActive != null) {
        connectionPool.maxActive = config.pool.maxActive
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

}
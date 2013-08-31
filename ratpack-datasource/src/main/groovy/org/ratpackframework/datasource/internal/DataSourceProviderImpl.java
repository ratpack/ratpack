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
package org.ratpackframework.datasource.internal;

import com.google.inject.Inject;
import groovy.util.ConfigObject;
import org.ratpackframework.datasource.DataSourceConnector;
import org.ratpackframework.datasource.DataSourceProvider;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Andres Almiray
 * @author Lukasz Pielak
 * @author Tom Bujok
 * @author Dmitry Vyazelenko
 */
public class DataSourceProviderImpl implements DataSourceProvider {
  private static final String DEFAULT = "default";
  private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
  private final DataSourceConnector dataSourceConnector;

  @Inject
  public DataSourceProviderImpl(DataSourceConnector dataSourceConnector) {
    this.dataSourceConnector = dataSourceConnector;
  }

  @Override
  public String[] getDataSourceNames() {
    List<String> dataSourceNames = new ArrayList<>();
    dataSourceNames.addAll(dataSources.keySet());
    return dataSourceNames.toArray(new String[dataSourceNames.size()]);
  }

  private String normalize(String dataSourceName) {
    return dataSourceName == null || dataSourceName.isEmpty() ? DEFAULT : dataSourceName;
  }

  @Override
  public DataSource getDataSource() {
    return getDataSource(DEFAULT);
  }

  @Override
  public DataSource getDataSource(String dataSourceName) {
    return dataSources.get(normalize(dataSourceName));
  }

  @Override
  public void setDataSource(DataSource ds) {
    setDataSource(DEFAULT, ds);
  }

  @Override
  public void setDataSource(String dataSourceName, DataSource ds) {
    dataSources.put(normalize(dataSourceName), ds);
  }

  @Override
  public boolean isDataSourceConnected(String dataSourceName) {
    return dataSources.containsKey(normalize(dataSourceName));
  }

  @Override
  public void disconnectDataSource(String dataSourceName) {
    dataSources.remove(normalize(dataSourceName));
  }

  @Override
  public DataSource fetchDataSource(String dataSourceName) {
    dataSourceName = normalize(dataSourceName);
    DataSource ds = dataSources.get(dataSourceName);
    if (null == ds) {
      ConfigObject config = dataSourceConnector.createConfig();
      ds = dataSourceConnector.connect(config, dataSourceName);
      dataSources.put(dataSourceName, ds);
    }

    if (null == ds) {
      throw new IllegalArgumentException("No such DataSource configuration for name '" + dataSourceName + "'");
    }
    return ds;
  }
}

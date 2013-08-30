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
package org.ratpackframework.jdbi.internal;

import org.ratpackframework.jdbi.DBIProvider;
import org.skife.jdbi.v2.DBI;

/**
 * @author Andres Almiray
 * @author Lukasz Pielak
 * @author Tom Bujok
 * @author Dmitry Vyazelenko
 */
public class DBIProviderImpl implements DBIProvider {

  @Override
  public DBI getDBI() {
    return getDBI("default");
  }

  @Override
  public DBI getDBI(String dataSourceName) {
    if (dataSourceName == null || dataSourceName.trim().length() == 0) {
      dataSourceName = "default";
    }
    return new DBI(DataSourceHolder.getInstance().fetchDataSource(dataSourceName));
  }

}

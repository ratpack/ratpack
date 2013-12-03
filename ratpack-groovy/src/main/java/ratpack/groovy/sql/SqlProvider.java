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

package ratpack.groovy.sql;

import groovy.sql.Sql;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

public class SqlProvider implements Provider<Sql> {

  private final DataSource ds;

  @Inject
  public SqlProvider(DataSource ds) {
    this.ds = ds;
  }

  @Override
  public Sql get() {
    return new Sql(ds);
  }

}

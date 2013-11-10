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
dataSource {
  url = "jdbc:h2:mem:jdbi"
  driverClassName = "org.h2.Driver"
  username = "sa"
  password = ""

  pool {
    maxWait = 60000
    maxIdle = 5
    maxActive = 8
  }

}

dataSources {

  prod {
    url = "jdbc:h2:mem:jdbi-prod"
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""

    pool {
      maxWait = 60000
      maxIdle = 5
      maxActive = 8
    }


  }


}

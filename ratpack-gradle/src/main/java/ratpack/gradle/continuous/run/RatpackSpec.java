/*
 * Copyright 2015 the original author or authors.
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

package ratpack.gradle.continuous.run;

import java.io.Serializable;
import java.net.URL;

public class RatpackSpec implements Serializable {

  private final URL[] classpath;
  private final URL[] changingClasspath;
  private final String mainClass;
  private final String[] args;

  public RatpackSpec(URL[] classpath, URL[] changingClasspath, String mainClass, String[] args) {
    this.classpath = classpath;
    this.changingClasspath = changingClasspath;
    this.mainClass = mainClass;
    this.args = args;
  }

  public URL[] getClasspath() {
    return classpath;
  }

  public URL[] getChangingClasspath() {
    return changingClasspath;
  }

  public String getMainClass() {
    return mainClass;
  }

  public String[] getArgs() {
    return args;
  }
}

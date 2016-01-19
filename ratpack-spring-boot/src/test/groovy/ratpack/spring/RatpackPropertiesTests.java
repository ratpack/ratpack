/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.spring;

import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import ratpack.spring.config.RatpackProperties;

public class RatpackPropertiesTests {

  private RatpackProperties ratpack = new RatpackProperties();

  @Test
  public void defaultBaseDirIsAbsolute() {
    assertTrue(ratpack.getBasepath().isAbsolute());
  }

  @Test
  public void defaultBaseDirIsAbsoluteWhenInJar() throws Exception {
    Resource basedir = new ClassPathResource("META-INF/io.netty.versions.properties");
    URI uri = basedir.getURI();
    assertTrue(uri.toString().startsWith("jar:file"));
    basedir = new UrlResource(uri.toString().replace("META-INF/io.netty.versions.properties", ""));
    ratpack.setBasedir(basedir);
    assertTrue(ratpack.getBasepath().isAbsolute());
  }

}

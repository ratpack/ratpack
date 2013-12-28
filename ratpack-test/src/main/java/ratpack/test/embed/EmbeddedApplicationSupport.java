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

package ratpack.test.embed;

import ratpack.server.RatpackServer;

import java.io.File;
import java.net.URI;

import static ratpack.util.ExceptionUtils.uncheck;

public abstract class EmbeddedApplicationSupport implements EmbeddedApplication {

  private final File baseDir;
  private RatpackServer ratpackServer;

  public EmbeddedApplicationSupport(File baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public File getBaseDir() {
    return baseDir;
  }

  @Override
  public File file(String path) {
    File file = new File(getBaseDir(), path);
    if (!file.getParentFile().mkdirs() && !file.getParentFile().exists()) {
      throw new IllegalStateException("Couldn't create directory: " + file.getParentFile());
    }
    return file;
  }

  @Override
  public RatpackServer getServer() {
    if (ratpackServer == null) {
      ratpackServer = createServer();
    }

    return ratpackServer;
  }

  @Override
  public URI getAddress() {
    RatpackServer server = getServer();
    try {
      if (!server.isRunning()) {
        server.start();
      }
      return new URI(server.getScheme(), null, server.getBindHost(), server.getBindPort(), "/", null, null);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  abstract protected RatpackServer createServer();

}

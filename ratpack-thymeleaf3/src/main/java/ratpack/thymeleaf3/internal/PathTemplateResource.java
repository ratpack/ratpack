/*
 * Copyright 2018 the original author or authors.
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

package ratpack.thymeleaf3.internal;

import org.thymeleaf.templateresource.ITemplateResource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PathTemplateResource implements ITemplateResource {

  private final String name;
  private final Path path;
  private final Charset charset;

  public PathTemplateResource(String name, Path path, Charset encoding) {
    this.name = name;
    this.path = path;
    this.charset = encoding;
  }

  @Override
  public String getDescription() {
    return path.toAbsolutePath().toString();
  }

  @Override
  public String getBaseName() {
    return name;
  }

  @Override
  public boolean exists() {
    return Files.exists(path);
  }

  @Override
  public Reader reader() throws IOException {
    return Files.newBufferedReader(path, charset);
  }

  @Override
  public ITemplateResource relative(String relativeLocation) {
    return new PathTemplateResource(name, path.getParent().resolve(relativeLocation), charset);
  }

}

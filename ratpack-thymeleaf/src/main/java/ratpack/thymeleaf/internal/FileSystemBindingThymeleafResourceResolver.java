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

package ratpack.thymeleaf.internal;

import org.thymeleaf.TemplateProcessingParameters;
import org.thymeleaf.resourceresolver.IResourceResolver;
import ratpack.file.FileSystemBinding;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemBindingThymeleafResourceResolver implements IResourceResolver {

  private final FileSystemBinding fileSystemBinding;

  public FileSystemBindingThymeleafResourceResolver(FileSystemBinding fileSystemBinding) {
    this.fileSystemBinding = fileSystemBinding;
  }

  @Override
  public String getName() {
    return getClass().getName();
  }

  @Override
  public InputStream getResourceAsStream(TemplateProcessingParameters templateProcessingParameters, String resourceName) {
    Path path = fileSystemBinding.file(resourceName);
    if (path == null) {
      return null;
    } else {
      try {
        return Files.newInputStream(path);
      } catch (IOException e) {
        return null; // unsure what to do here, log?
      }
    }
  }
}

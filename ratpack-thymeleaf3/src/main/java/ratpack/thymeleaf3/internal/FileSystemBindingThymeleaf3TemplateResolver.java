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

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import ratpack.file.FileSystemBinding;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;

public final class FileSystemBindingThymeleaf3TemplateResolver extends AbstractConfigurableTemplateResolver {

  private final FileSystemBinding fileSystemBinding;

  public FileSystemBindingThymeleaf3TemplateResolver(FileSystemBinding fileSystemBinding) {
    this.fileSystemBinding = fileSystemBinding;
  }

  @Override
  protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate, String template, String resourceName, String characterEncoding, Map<String, Object> templateResolutionAttributes) {
    Path path = fileSystemBinding.file(resourceName);
    if (path == null) {
      return null;
    } else {
      Charset charset = characterEncoding == null ? Charset.defaultCharset() : Charset.forName(characterEncoding);
      return new PathTemplateResource(template, path, charset);
    }
  }

}

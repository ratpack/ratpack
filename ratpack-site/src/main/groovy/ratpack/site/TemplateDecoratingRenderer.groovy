/*
 * Copyright 2014 the original author or authors.
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

package ratpack.site

import com.google.inject.Inject
import groovy.text.markup.MarkupTemplateEngine
import ratpack.groovy.markuptemplates.MarkupTemplate
import ratpack.groovy.markuptemplates.internal.MarkupTemplateRenderer
import ratpack.handling.Context

class TemplateDecoratingRenderer extends MarkupTemplateRenderer {

  private final AssetLinkService assetLinkService

  @Inject
  TemplateDecoratingRenderer(MarkupTemplateEngine engine, AssetLinkService assetLinkService) {
    super(engine)
    this.assetLinkService = assetLinkService
  }

  @Override
  void render(Context context, MarkupTemplate template) throws Exception {
    def decoratedTemplate = new MarkupTemplate(template.name, template.contentType, template.model + [assets: assetLinkService])
    super.render(context, decoratedTemplate)
  }

}

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

package ratpack.manual.snippets.extractor

import ratpack.exec.util.RatpackVersion
import ratpack.test.docs.extractor.MarkdownSnippetExtractor


class ManualSnippetExtractor extends MarkdownSnippetExtractor {

  /**
   * Perform the substitutions that {@code :ratpack-manual:tokeniseManual} would perform, as required by tested snippets.
   */
  @Override
  protected String performSubstitutions(String snippet) {
    return snippet
      .replaceAll("@ratpack-version@", RatpackVersion.getVersion())
      .replaceAll("@shadow-version@", "4.0.3")
  }

}

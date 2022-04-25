/*
 * Copyright 2022 the original author or authors.
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

package ratpack.test.docs;

import ratpack.test.docs.executer.ExtractedSnippet;

public interface SnippetExecuter {

  SnippetFixture getFixture();

  void execute(TestCodeSnippet snippet) throws Exception;

  default ExtractedSnippet extractImports(String snippet) {
    StringBuilder imports = new StringBuilder();
    StringBuilder scriptMinusImports = new StringBuilder();

    for (String line : snippet.split("\\n")) {
      String trimmedLine = line.trim();
      if (trimmedLine.startsWith("package ") || trimmedLine.startsWith("import ")) {
        imports.append(line).append("\n");
      } else {
        scriptMinusImports.append(line).append("\n");
      }
    }

    return new ExtractedSnippet(imports.toString(), scriptMinusImports.toString());
  }

}

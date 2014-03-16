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

package ratpack.manual.snippets.fixtures

class GradleFixture implements SnippetFixture {
  @Override
  void setup() {
  }

  @Override
  void cleanup() {
  }

  @Override
  String pre() {
"""
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.GradleProject

def script = '''
"""
  }

  @Override
  String post() {
"""
'''
def dir = File.createTempDir()
def buildFile = new File(dir, "build.gradle")
try {
  buildFile.text = script
  def connector = GradleConnector.newConnector().forProjectDirectory(dir).useGradleVersion("1.11")
  def connection = connector.connect()
  try {
    connection.getModel(GradleProject)
  } finally {
    connection.close()
  }
} finally {
  dir.deleteDir()
}
"""
  }
}

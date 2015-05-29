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

package ratpack.manual.snippets.fixture

import ratpack.func.Block

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class GradleFixture extends GroovyScriptFixture {

  private final Lock lock = new ReentrantLock()
  private final Condition available = lock.newCondition()
  private volatile boolean busy

  @Override
  void around(Block action) throws Exception {
    try {
      lock.lock()
      while (busy) {
        available.await()
      }
      busy = true
      action.execute()
    } finally {
      busy = false
      available.signal()
      lock.unlock()
    }
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
    String localRepo = System.getProperty("localRepo", "../build/localrepo")
    def localRepoPath = new File(localRepo).canonicalPath?.replaceAll("\\\\", "/")
    """
'''
def projectDir = File.createTempDir()
def buildFile = new File(projectDir, "build.gradle")
try {
  buildFile.text = 'buildscript { repositories { maven { url "file://${localRepoPath}" } } }\\n' + script
  def connector = (org.gradle.tooling.internal.consumer.DefaultGradleConnector) GradleConnector.newConnector().forProjectDirectory(projectDir)

  def gradleUserHome = System.getProperty("gradleUserHome")
  if (gradleUserHome) {
    connector.useGradleUserHomeDir(new File(gradleUserHome))
  }

  def gradleHome = System.getProperty("gradleHome")
  if (gradleHome) {
    connector.useInstallation(new File(gradleHome))
  }

  connector.embedded(true)
  def connection = connector.connect()
  try {
    connection.getModel(GradleProject)
  } finally {
    connection.close()
  }
} finally {
  projectDir.deleteDir()
}
"""
  }
}

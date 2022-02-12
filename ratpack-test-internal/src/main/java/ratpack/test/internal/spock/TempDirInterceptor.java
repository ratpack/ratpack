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

package ratpack.test.internal.spock;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.spockframework.runtime.extension.IMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.model.FieldInfo;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

import static java.nio.file.FileVisitResult.CONTINUE;

// This is copied from the org.spockframework.runtime.extension.builtin.TempDirInterceptor since it's constructor
// is package private.
public class TempDirInterceptor implements IMethodInterceptor {

  private static final String TEMP_DIR_PREFIX = "spock";
  private static final Pattern VALID_CHARS = Pattern.compile("[^a-zA-Z0-9_.-]++");

  private final Class<?> fieldType;
  private final FieldInfo fieldInfo;
  private final Path parentDir;
  private final boolean keep;

  TempDirInterceptor(Class<?> fieldType, FieldInfo fieldInfo,
                     Path parentDir, boolean keep) {
    this.fieldType = fieldType;
    this.fieldInfo = fieldInfo;
    this.parentDir = parentDir;
    this.keep = keep;
  }

  private String dirPrefix(IMethodInvocation invocation) {
    StringBuilder prefix = new StringBuilder(TEMP_DIR_PREFIX);
    prefix.append('_');
    // for shared field, no iteration is set, so use the spec name
    // otherwise use the iteration name
    String displayName = (invocation.getIteration() == null)
      ? invocation.getSpec().getDisplayName()
      : invocation.getIteration().getDisplayName();
    prefix.append(VALID_CHARS.matcher(displayName).replaceAll("_"));
    if (prefix.length() > 25) {
      prefix.setLength(25);
    }
    if (invocation.getIteration() != null) {
      prefix.append('_').append(invocation.getIteration().getIterationIndex());
    }
    return prefix.append('_').append(fieldInfo.getName()).toString();
  }

  private Path generateTempDir(IMethodInvocation invocation) throws IOException {
    String prefix = dirPrefix(invocation);
    if (parentDir == null) {
      return Files.createTempDirectory(prefix);
    }
    return Files.createTempDirectory(parentDir, prefix);
  }

  protected Path setUp(IMethodInvocation invocation) throws IOException {
    Path tempPath = generateTempDir(invocation);
    TemporaryFolder temporaryFolder = new TemporaryFolder(tempPath.toFile());
    fieldInfo.writeValue(invocation.getInstance(), temporaryFolder);
    return tempPath;
  }

  protected void destroy(Path path) throws IOException {
    if (!keep) {
      deleteTempDir(path);
    }
  }

  @Override
  public void intercept(IMethodInvocation invocation) throws Throwable {
    Path path = setUp(invocation);
    try {
      invocation.proceed();
    } finally {
      destroy(path);
    }
  }

  private void deleteTempDir(Path tempPath) throws IOException {
    if (Files.notExists(tempPath)) {
      return;
    }

    if (ResourceGroovyMethods.deleteDir(tempPath.toFile())) {
      return;
    }

    tryMakeWritable(tempPath);
    ResourceGroovyMethods.deleteDir(tempPath.toFile());
  }

  private void tryMakeWritable(Path tempPath) throws IOException {
    Files.walkFileTree(tempPath, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        file.toFile().setWritable(true);
        return CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        dir.toFile().setWritable(true);
        return CONTINUE;
      }
    });
  }
}

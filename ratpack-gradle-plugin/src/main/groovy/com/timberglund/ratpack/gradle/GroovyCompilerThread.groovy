package com.timberglund.ratpack.gradle

import org.gradle.api.AntBuilder;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ClassPathRegistry;
import org.gradle.api.internal.file.TemporaryFileProvider;
import org.gradle.api.internal.project.IsolatedAntBuilder;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.compile.*;
import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.Factory;

class GroovyCompilerThread
  extends Thread {

  def compileGroovy
  def compiler
  def tempFileProvider
  FileCollection groovyClasspath

  public GroovyCompile() {
    ProjectInternal projectInternal = project
    IsolatedAntBuilder antBuilder = services.get(IsolatedAntBuilder.class)
    ClassPathRegistry classPathRegistry = services.get(ClassPathRegistry.class)
    Factory<AntBuilder> antBuilderFactory = services.getFactory(AntBuilder.class)
    JavaCompilerFactory inProcessCompilerFactory = new InProcessJavaCompilerFactory()
    tempFileProvider = projectInternal.services.get(TemporaryFileProvider.class)
    DefaultJavaCompilerFactory javaCompilerFactory = new DefaultJavaCompilerFactory(projectInternal, tempFileProvider, antBuilderFactory, inProcessCompilerFactory)
    GroovyCompilerFactory groovyCompilerFactory = new GroovyCompilerFactory(projectInternal, antBuilder, classPathRegistry, javaCompilerFactory)
    Compiler<GroovyJavaJointCompileSpec> delegatingCompiler = new DelegatingGroovyCompiler(groovyCompilerFactory)
    compiler = new IncrementalGroovyCompiler(delegatingCompiler, outputs)
  }

  def filesOutOfDate() {
    return true
  }  

  public void run() {
    if(filesOutOfDate()) {
      List<File> taskClasspath = new ArrayList<File>(getGroovyClasspath().getFiles());
      throwExceptionIfTaskClasspathIsEmpty(taskClasspath);
      DefaultGroovyJavaJointCompileSpec spec = new DefaultGroovyJavaJointCompileSpec();
      spec.source =compileGroovy.source
      spec.destinationDir = compileGroovy.destinationDir
      spec.classpath = compileGroovy.classpath
      spec.sourceCompatibility = compileGroovy.sourceCompatibility
      spec.targetCompatibility = compileGroovy.targetCompatibility
      spec.groovyClasspath = compileGroovy.taskClasspath
      spec.compileOptions = compileGroovy.compileOptions
      spec.groovyCompileOptions = compileGroovy.groovyCompileOptions
      if(spec.groovyCompileOptions.stubDir == null) {
          File dir = tempFileProvider.newTemporaryFile("groovy-java-stubs");
          dir.mkdirs();
          spec.groovyCompileOptions().stubDir = dir;
      }
      WorkResult result = compiler.execute(spec);
    }
  }
}

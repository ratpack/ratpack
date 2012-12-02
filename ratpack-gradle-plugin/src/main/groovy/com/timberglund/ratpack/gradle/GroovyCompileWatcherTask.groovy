package com.timberglund.ratpack.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.tasks.compile.GroovyCompileOptions
import org.gradle.api.internal.tasks.compile.DefaultGroovyJavaJointCompileSpec
import org.gradle.api.internal.tasks.compile.DelegatingGroovyCompiler
import org.gradle.api.internal.tasks.compile.JavaCompilerFactory
import org.gradle.api.internal.tasks.compile.InProcessJavaCompilerFactory
import org.gradle.api.internal.tasks.compile.DefaultJavaCompilerFactory
import org.gradle.api.internal.tasks.compile.GroovyCompilerFactory
import org.gradle.api.internal.tasks.compile.GroovyJavaJointCompileSpec
import org.gradle.api.internal.tasks.compile.IncrementalGroovyCompiler
import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.internal.file.TemporaryFileProvider;
import org.gradle.api.internal.ClassPathRegistry
import org.gradle.api.tasks.WorkResult


class GroovyCompileWatcherTask 
  extends DefaultTask {

  GroovyCompile compileGroovy

  def sourceFileTimestamp = [:]
  def compiler
  def tempFileProvider
  def final compileOptions = new CompileOptions()
  def final groovyCompileOptions = new GroovyCompileOptions()

  def init() {
    ProjectInternal projectInternal = compileGroovy.project
    IsolatedAntBuilder antBuilder = compileGroovy.services.get(IsolatedAntBuilder.class)
    ClassPathRegistry classPathRegistry = compileGroovy.services.get(ClassPathRegistry.class)
    def antBuilderFactory = compileGroovy.services.getFactory(AntBuilder.class)
    JavaCompilerFactory inProcessCompilerFactory = new InProcessJavaCompilerFactory()
    tempFileProvider = projectInternal.services.get(TemporaryFileProvider.class)
    DefaultJavaCompilerFactory javaCompilerFactory = new DefaultJavaCompilerFactory(projectInternal, tempFileProvider, antBuilderFactory, inProcessCompilerFactory)
    GroovyCompilerFactory groovyCompilerFactory = new GroovyCompilerFactory(projectInternal, antBuilder, classPathRegistry, javaCompilerFactory)
    Compiler<GroovyJavaJointCompileSpec> delegatingCompiler = new DelegatingGroovyCompiler(groovyCompilerFactory)
    compiler = new IncrementalGroovyCompiler(delegatingCompiler, compileGroovy.outputs)
  }

  def fileTreeOutOfDate(fileTree) {
    def outOfDate = false
    println "CHECKING OUT-OF-DATENESS OF ${fileTree.files}"
    fileTree.visit { fileVisitDetails ->
      println fileVisitDetails
      def oldDate = sourceFileTimestamp[fileVisitDetails.path]
      def currentDate = new Date(fileVisitDetails.file.lastModified())
      println "${fileVisitDetails.path} == ${oldDate}/${currentDate}"
      if(oldDate == null || oldDate.before(currentDate)) {
        outOfDate = true
      }
      sourceFileTimestamp[fileVisitDetails.path] = currentDate
    }
    return outOfDate
  }


  @TaskAction
  def spawnThread() {
    println "SPAWNING GroovyCompileWatcher THREAD"
    init()
    Thread.start {
      while(true) {
        if(fileTreeOutOfDate(compileGroovy.source)) {
          println "RECOMPILING"
          compile()
        }

        if(fileTreeOutOfDate(project.sourceSets.app.allGroovy)) {
          println "APP SCRIPT OUT OF DATE"
        }

        Thread.sleep(1000)
      }
    }
  }

  def compile() {
    List<File> taskClasspath = new ArrayList<File>(compileGroovy.groovyClasspath.files)
    throwExceptionIfTaskClasspathIsEmpty(taskClasspath)
    def spec = new DefaultGroovyJavaJointCompileSpec()
    spec.source = compileGroovy.source
    spec.destinationDir = compileGroovy.destinationDir
    spec.classpath = compileGroovy.classpath
    spec.sourceCompatibility = compileGroovy.sourceCompatibility
    spec.targetCompatibility = compileGroovy.targetCompatibility
    spec.groovyClasspath = taskClasspath
    spec.compileOptions = compileOptions
    spec.groovyCompileOptions = groovyCompileOptions
    if(spec.groovyCompileOptions.stubDir == null) {
        File dir = tempFileProvider.newTemporaryFile("groovy-java-stubs");
        dir.mkdirs()
        spec.groovyCompileOptions.stubDir = dir
    }
    compiler.execute(spec)
  }

  def throwExceptionIfTaskClasspathIsEmpty(taskClasspath) {
    if(taskClasspath.size() == 0) {
      throw new InvalidUserDataException("You must assign a Groovy library to the 'groovy' configuration.")
    }
  }


}

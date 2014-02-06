package ratpack.gradle.task

import groovy.text.SimpleTemplateEngine
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction

class GenerateBuildpackFiles extends DefaultTask {

  private final static List<String> buildpackFilePaths = ['bin/compile', 'bin/detect', 'bin/release', '.profile.d/setenv.sh']

  @OutputFiles
  FileCollection getOutputFiles() {
    project.rootProject.files(*buildpackFilePaths)
  }

  @Input
  String getInstallAppPath() {
    project.tasks.getByName('installApp').path
  }

  @Input
  String getRelativeRunScriptPath() {
    def applicationConvention = project.convention.plugins.get('application')
    Sync installAppTask = project.tasks.getByName('installApp')
    def binDir = new File(installAppTask.destinationDir, 'bin')
    def scriptFile = new File(binDir, applicationConvention.applicationName)
    project.rootDir.toURI().relativize(scriptFile.toURI()).path
  }

  @TaskAction
  void generate() {
    def engine = new SimpleTemplateEngine()

    buildpackFilePaths.each { path ->
      def templateReader = new InputStreamReader(getClass().getResourceAsStream("/heroku/$path"))
      def template = engine.createTemplate(templateReader)
      project.rootProject.file(path).withWriter { writer ->
        template.make(
          installAppPath: installAppPath,
          relativeRunScriptPath: relativeRunScriptPath
        ).writeTo(writer)
      }
    }
  }
}

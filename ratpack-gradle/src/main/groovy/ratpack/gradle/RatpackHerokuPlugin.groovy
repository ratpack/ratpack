package ratpack.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import ratpack.gradle.task.GenerateBuildpackFiles

class RatpackHerokuPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.plugins.apply(ApplicationPlugin)

    project.task('generateBuildpackFiles', type: GenerateBuildpackFiles)
  }
}

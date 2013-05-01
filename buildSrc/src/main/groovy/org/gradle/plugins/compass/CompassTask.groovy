package org.gradle.plugins.compass

import org.gradle.api.tasks.*
import static org.gradle.plugins.compass.CompassPlugin.CONFIGURATION_NAME

class CompassTask extends JRubyTask {

	String command
	boolean background

	@InputDirectory
	File gemPath

	@OutputDirectory
	File cssDir

	@InputDirectory
	File sassDir

	@InputDirectory
	File imagesDir

	@InputDirectory
	File javascriptsDir

	CompassTask() {
		doFirst {
			getCssDir().mkdirs()
		}
	}

	@Override
	protected Iterable<String> getJRubyArguments() {
		def args = []
		args << '-X-C'
		args << '-S' << 'compass' << command
		args << '--sass-dir' << getSassDir()
		args << '--css-dir' << getCssDir()
		args << '--images-dir' << getImagesDir()
		args << '--javascripts-dir' << getJavascriptsDir()
		return args
	}

	@TaskAction
	@Override
	void jrubyexec() {
		if (background) {
			Thread.start {
				super.jrubyexec()
			}
		} else {
			super.jrubyexec()
		}
	}
}
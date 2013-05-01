package org.gradle.plugins.compass

import org.gradle.api.tasks.OutputDirectory

class InstallGems extends JRubyTask {

	@OutputDirectory
	File gemPath
	String gems

	InstallGems() {
		doFirst {
			getGemPath().mkdirs()
		}
	}

	protected Iterable<String> getJRubyArguments() {
		def args = []
		args << '-X-C'
		args << '-S' << 'gem' << 'install'
		args << '-i' << getGemPath()
		args << '--no-rdoc'
		args << '--no-ri'
		args << gems
		return args
	}
}

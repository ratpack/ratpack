apply plugin: "groovy"

dependencies {
  groovy commonDependencies.groovy
  testCompile commonDependencies.spock
  testCompile project(":ratpack-test-support")
}

configurations {
  compile.extendsFrom -= groovy
  testCompile.extendsFrom groovy
}

apply from: "$rootDir/gradle/checkstyle.gradle"
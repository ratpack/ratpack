/*
 * Copyright 2021 the original author or authors.&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;   http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package com.eriwen.gradle.js

import com.google.javascript.jscomp.CommandLineRunner
import com.google.javascript.jscomp.CompilationLevel
import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.CompilerOptions
import com.google.javascript.jscomp.SourceFile
import com.google.javascript.jscomp.Result
import com.google.javascript.jscomp.WarningLevel
import org.gradle.api.GradleException

/**
 * Util to minify JS files with Google Closure Compiler.
 *
 * @author Eric Wendelin
 * @date 10/30/11
 *
 * PATCH: https://github.com/eriwen/gradle-js-plugin/issues/170
 */
class JsMinifier {

    void minifyJsFile(final Set<File> inputFiles, final Set<File> externsFiles, final File outputFile, final File sourceMap, CompilerOptions options,
            final String warningLevel, final String compilationLevel) {
        options = options ?: new CompilerOptions()
        options.setSourceMapOutputPath(sourceMap?.path)
        Compiler compiler = new Compiler()
        CompilationLevel.valueOf(compilationLevel).setOptionsForCompilationLevel(options)
        WarningLevel level = WarningLevel.valueOf(warningLevel)
        level.setOptionsForWarningLevel(options)
        List<SourceFile> externs = CommandLineRunner.getBuiltinExterns(options.environment);
        if (externsFiles.size()) {
            externs.addAll(externsFiles.collect() { SourceFile.fromFile(it) })
        }
        List<SourceFile> inputs = new ArrayList<SourceFile>()
        inputFiles.each { inputFile ->
          inputs.add(SourceFile.fromFile(inputFile.absolutePath))
        }
        Result result = compiler.compile(externs, inputs, options)
        if (result.success) {
            outputFile.write(compiler.toSource())
            if(sourceMap) {
              def sourceMapContent = new StringBuffer()
              result.sourceMap.appendTo(sourceMapContent, outputFile.name)
              sourceMap.write(sourceMapContent.toString())
            }
        } else {
        	String error = ""
            result.errors.each {
                error += "${it.sourceName}:${it.lineNumber} - ${it.description}\n"
            }
            throw new GradleException(error)
        }
    }
}

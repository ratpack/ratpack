/*
 * Copyright 2013 the original author or authors.
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

package org.ratpackframework.site

import org.ratpackframework.test.RequestingSpec
import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.groovy.RatpackScriptApp



class SiteSmokeSpec extends RequestingSpec {

		
  @Override
  protected RatpackServer createServer() {
  	//It doesn't seem like I should have to do this work everytime is there a spec type I should be extending?
  	def file = new File("src/ratpack/ratpack.groovy")
    return RatpackScriptApp.ratpack(file,file.getAbsoluteFile().getParentFile(),0,null,false,false)
  }


	def "Check Site Index"() {
		given:


		when:
		get("index.html")

		then:
		response.statusCode == 200
		response.body.asString().contains('<title>Ratpack: A toolkit for JVM web applications</title>')

	}

	def "Check Site /"() {
		given:


		when:
		get("")

		then:
		response.statusCode == 200
		response.body.asString().contains('<title>Ratpack: A toolkit for JVM web applications</title>')
	}	

}
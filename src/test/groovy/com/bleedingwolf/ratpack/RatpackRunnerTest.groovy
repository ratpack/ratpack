package com.bleedingwolf.ratpack;

import org.junit.Test
import org.junit.Before
import static org.junit.Assert.*
import groovy.lang.Binding

import groovy.mock.interceptor.MockFor

class RatpackRunnerTest {
	def scriptEngine = new MockFor(GroovyScriptEngine)
	def testFile = new File('name')
	def runner = new RatpackRunner()
	
	@Before
	public void setup() {
		testFile.metaClass.getCanonicalName = { '/a/path/to/name' }
		RatpackServlet.metaClass.'static'.serve = { }
	}
	@Test
	void useFileNameWhenRunning() {
		scriptEngine.demand.run(1) { filename, binding ->
			assert 'name' == filename
			assert binding instanceof Binding
		}
		
		scriptEngine.use {
			runner.run(testFile)
		}
	}
	
	@Test
	void bindRatpackAppHttpMethods() {
		def bindingReceived = null
		scriptEngine.demand.run(1) { filename, binding ->
			bindingReceived = binding
		}
		
		scriptEngine.use {
			runner.run(testFile)
		}
		
		assert runner.app.get == bindingReceived.getVariable('get')
		assert runner.app.post == bindingReceived.getVariable('post')
		assert runner.app.put == bindingReceived.getVariable('put')
		assert runner.app.delete == bindingReceived.getVariable('delete')
	}
	
	@Test
	void bindRatpackAppSetMethod() {
		def bindingReceived = null
		scriptEngine.demand.run(1) { filename, binding ->
			bindingReceived = binding
		}
		
		scriptEngine.use {
			runner.run(testFile)
		}
		
		assert runner.app.set == bindingReceived.getVariable('set')
	}
	
	@Test
	void startRatpackApp() {
		def appReceived = null
		RatpackServlet.metaClass.'static'.serve = { it ->
			appReceived = it
		}
		
		scriptEngine.demand.run(1) { filename, binding -> 
		}
		
		scriptEngine.use {
			runner.run(testFile)
		}
		
		assert runner.app == appReceived
	}
}
package com.bleedingwolf.ratpack

import groovy.mock.interceptor.MockFor
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class RatpackRunnerTest {
    def scriptEngine = new MockFor(GroovyScriptEngine)
    def testFile = new File('name')
    def runner

    @Before
    public void setup() {
        testFile.metaClass.getCanonicalName = { '/a/path/to/name' }
        RatpackServlet.metaClass.'static'.serve = { }
        runner = new RatpackRunner()
    }

    @After
    public void tearDown() {
        runner.stop()
    }

    @Test
    void useFileNameWhenRunning() {
        scriptEngine.demand.run(1) { filename, binding ->
            assertEquals 'name', filename
            assertTrue binding instanceof Binding
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

        assertEquals bindingReceived.getVariable('get'), runner.app.get
        assertEquals bindingReceived.getVariable('post'), runner.app.post
        assertEquals bindingReceived.getVariable('put'), runner.app.put
        assertEquals bindingReceived.getVariable('delete'), runner.app.delete
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
        assertEquals bindingReceived.getVariable('set'), runner.app.set
    }
}
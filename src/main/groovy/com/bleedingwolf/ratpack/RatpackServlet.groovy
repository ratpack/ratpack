
package com.bleedingwolf.ratpack

import java.io.File

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.Context
import org.mortbay.jetty.servlet.ServletHolder

class RatpackServlet extends HttpServlet {
    
    def app = null
    
    void init() {
    	if(app == null) {
	    	def appScriptName = getServletConfig().getInitParameter("app-script-filename")
			def fullScriptPath = getServletContext().getRealPath("WEB-INF/lib/${appScriptName}")
			
	    	println "Loading app from script '${appScriptName}'"
			loadAppFromScript(fullScriptPath)
		}
    }
    
    private void loadAppFromScript(filename) {
		ClassLoader parent = getClass().getClassLoader()
		GroovyClassLoader loader = new GroovyClassLoader(parent)
		Class groovyClass = loader.parseClass(new File(filename))
		
		GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
		app = groovyObject.run()    
    }
    
    void service(HttpServletRequest req, HttpServletResponse res) {
    
        def verb = req.method
        def path = req.pathInfo
    
        def renderer = new TemplateRenderer(app.config.templateRoot)
        
        def handler = app.getHandler(verb, path)
        def output = ''
        
        if(handler) {
                
            handler.delegate.renderer = renderer
            handler.delegate.request = req
            handler.delegate.response = res
            
            try {
                output = handler.call()
            }
            catch(RuntimeException ex) {
            
                // FIXME: use log4j or similar
                println "[ Error] Caught Exception: ${ex}"
                
                res.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                output = renderer.renderException(ex, req)
            }
            
        } else if(app.config.public && staticFileExists(path)){
        
            output = serveStaticFile(path)
            
        } else {

            res.status = HttpServletResponse.SC_NOT_FOUND
            
            output = renderer.render('exception.html', [
                title: 'Page Not Found',
                message: 'Page Not Found',
                metadata: [
                    'Request Method': req.method.toUpperCase(),
                    'Request URL': req.requestURL,
                ]
            ])

        }
        
        output = convertOutputToByteArray(output)
            
        def contentLength = output.length
        res.setHeader('Content-Length', contentLength.toString())
        
        def stream = res.getOutputStream()
        stream.write(output)
        stream.flush()
        stream.close()
        
        // FIXME: use log4j or similar
        println "[   ${res.status}] ${verb} ${path}"
    }
    
    private boolean staticFileExists(url) {
        def publicDir = app.config.public
        def fullPath = [publicDir, url].join(File.separator)
        def file = new File(fullPath)
	// For now, directory listings shall not be served.
	return file.exists() && !file.isDirectory()
    }
    
    private def serveStaticFile(url) {
        def publicDir = app.config.public
        def fullPath = [publicDir, url].join(File.separator)
        def file = new File(fullPath)
        
        file.getBytes()
    }
    
    private byte[] convertOutputToByteArray(output) {
        if(output instanceof String)
            output = output.getBytes()
        else if(output instanceof GString)
            output = output.toString().getBytes()
        return output
    }
    
    static void serve(theApp, port=5000) {
        // Runs this RatpackApp in a Jetty container
        
        def servlet = new RatpackServlet()
        servlet.app = theApp
        
        println "Starting Ratpack app with config:"
        println theApp.config
        
        def server = new Server(port)
        def root = new Context(server, "/", Context.SESSIONS)
        root.addServlet(new ServletHolder(servlet), "/*")
        server.start()
    }
}

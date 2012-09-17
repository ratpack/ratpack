package com.bleedingwolf.ratpack

import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.Context
import org.mortbay.jetty.servlet.ServletHolder


class RatpackRunner {
    RatpackApp app = new RatpackApp()
    Server server

    void run(File scriptFile) {
        app.prepareScriptForExecutionOnApp(scriptFile)
        app.config.templateRoot = 'src/app/resources/templates'

        // Runs this RatpackApp in a Jetty container
        def servlet = new RatpackServlet()
        servlet.app = app

        app.logger.info('Starting Ratpack app with config:\n{}', app.config)

        server = new Server(app.config.port)
        def root = new Context(server, "/", Context.SESSIONS)
        root.addServlet(new ServletHolder(servlet), "/*")
        println "starting server"
        server.start()
        println "done starting server"
    }

    void stop() {
        server.stop()
    }

    static void main(args) {
        def runner = new RatpackRunner()
        runner.run(new File(args[0]))
    }
}

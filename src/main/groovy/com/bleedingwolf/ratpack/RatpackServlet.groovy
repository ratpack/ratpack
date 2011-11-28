package com.bleedingwolf.ratpack

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.slf4j.LoggerFactory

class RatpackServlet extends HttpServlet {
    protected final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass())

    def app = null
    MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap()

    void init() {
        if(app == null) {
            def appScriptName = getServletConfig().getInitParameter("app-script-filename")
            def fullScriptPath = getServletContext().getRealPath("WEB-INF/scripts/${appScriptName}")

            logger.info('Loading app from script "{}"', appScriptName)
            loadAppFromScript(fullScriptPath)
        }
        mimetypesFileTypeMap.addMimeTypes(this.class.getResourceAsStream('mime.types').text)
    }

    private void loadAppFromScript(filename) {
        app = new RatpackApp()
        app.prepareScriptForExecutionOnApp(filename)
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
                logger.error('Handling {} {}', [ verb, path, ex] as Object[])

                res.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                output = renderer.renderException(ex, req)
            }

        } else if(app.config.public && staticFileExists(path)) {

            output = serveStaticFile(res, path)

        } else {
            res.status = HttpServletResponse.SC_NOT_FOUND
            output = renderer.renderError(
               title: 'Page Not Found',
               message: 'Page Not Found',
               metadata: [
                   'Request Method': req.method.toUpperCase(),
                   'Request URL': req.requestURL,
               ]
            )
        }

        output = convertOutputToByteArray(output)

        def contentLength = output.length
        res.setHeader('Content-Length', contentLength.toString())

        def stream = res.getOutputStream()
        stream.write(output)
        stream.flush()
        stream.close()

        logger.info("[   ${res.status}] ${verb} ${path}")
    }

    protected boolean staticFileExists(path) {
        !path.endsWith('/') && staticFileFrom(path) != null
    }

    protected def serveStaticFile(response, path) {
        URL url = staticFileFrom(path)
        response.setHeader('Content-Type', mimetypesFileTypeMap.getContentType(url.toString()))
        url.openStream().bytes
    }

    protected URL staticFileFrom(path) {
        def publicDir = app.config.public
        def fullPath = [publicDir, path].join(File.separator)
        def file = new File(fullPath)

        if(file.exists()) return file.toURI().toURL()

        try {
            return Thread.currentThread().contextClassLoader.getResource([publicDir, path].join('/'))
        } catch(Exception e) {
            return null
        }
    }

    private byte[] convertOutputToByteArray(output) {
        if(output instanceof String)
            output = output.getBytes()
        else if(output instanceof GString)
            output = output.toString().getBytes()
        return output
    }

}

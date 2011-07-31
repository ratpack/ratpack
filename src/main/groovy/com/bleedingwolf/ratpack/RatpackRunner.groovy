package com.bleedingwolf.ratpack;


class RatpackRunner {
	RatpackApp app = new RatpackApp()

	void run(File scriptFile) {
    app.prepareScriptForExecutionOnApp(scriptFile)
		RatpackServlet.serve(app)
	}
}

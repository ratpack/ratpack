import org.ratpackframework.app.*
import org.ratpackframework.groovy.app.Routing

(this as Routing).with {

	get('/') { Request request, Response response ->
		response.redirect('index.html')
	}

	get('/chapter1') { Request request, Response response ->
		render 'chapter1'
	}

}
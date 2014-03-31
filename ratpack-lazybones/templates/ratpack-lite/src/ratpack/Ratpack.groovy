import static ratpack.groovy.Groovy.ratpack

ratpack {
	handlers {
		get {
			redirect "index.html"
		}

		assets "public"
	}
}

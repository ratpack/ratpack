import java.text.SimpleDateFormat

set 'port', 4999

get("/") {
	def ua = headers['user-agent']
    "Your user-agent: ${ua}"
}
    
get("/foo/:name") {
	"Hello, ${urlparams.name}"
}
    
get("/person/:id") {
	"Person #${urlparams.id}"
}
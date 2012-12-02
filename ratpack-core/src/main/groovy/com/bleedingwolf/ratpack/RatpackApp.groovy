package com.bleedingwolf.ratpack

import com.bleedingwolf.ratpack.routing.Route
import com.bleedingwolf.ratpack.routing.RoutingTable
import org.slf4j.LoggerFactory

class RatpackApp {

  final logger = LoggerFactory.getLogger(getClass())

  def handlers = [
    'GET': new RoutingTable(),
    'POST': new RoutingTable(),
  ]

  def config = [
    port: 5000,
    templateRoot: 'templates'
  ]

  def set = { setting, value ->
    config[setting] = value
  }

  void register(List methods, path, handler) {
    methods.each {
      register(it, path, handler)
    }
  }

  void register(method, path, handler) {
    method = method.toUpperCase()

    if(path instanceof String) {
      path = new Route(path)
    }

    def routingTable = handlers[method]
    if(routingTable == null) {
      routingTable = new RoutingTable()
      handlers[method] = routingTable
    }
    routingTable.attachRoute path, handler
  }

  Closure getHandler(method, subject) {
    return handlers[method.toUpperCase()]?.route(subject)
  }

  def get = { path, handler ->
    register('GET', path, handler)
  }

  def post = { path, handler ->
    register('POST', path, handler)
  }

  def put = { path, handler ->
    register('PUT', path, handler)
  }

  def delete = { path, handler ->
    register('DELETE', path, handler)
  }

  void prepareScriptForExecutionOnApp(String scriptName) {
    prepareScriptForExecutionOnApp(new File(scriptName))
  }

  void prepareScriptForExecutionOnApp(File scriptFile) {
    scriptFile.eachFileRecurse { file ->
      if(file.isFile()) {
        runApplicationScript(file)
      }
    }
  }

  def runApplicationScript(File scriptFile) {
    GroovyScriptEngine gse = new GroovyScriptEngine(scriptFile.canonicalPath.replace(scriptFile.name,''))
    Binding binding = new Binding()
    binding.setVariable('get', this.get)
    binding.setVariable('post', this.post)
    binding.setVariable('put', this.put)
    binding.setVariable('delete', this.delete)
    binding.setVariable('set', this.set)
    gse.run(scriptFile.name, binding)
  }
}

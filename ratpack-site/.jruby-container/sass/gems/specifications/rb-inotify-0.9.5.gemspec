# -*- encoding: utf-8 -*-
# stub: rb-inotify 0.9.5 ruby lib

Gem::Specification.new do |s|
  s.name = "rb-inotify"
  s.version = "0.9.5"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Nathan Weizenbaum"]
  s.date = "2014-06-06"
  s.description = "A Ruby wrapper for Linux's inotify, using FFI"
  s.email = "nex342@gmail.com"
  s.extra_rdoc_files = ["README.md"]
  s.files = ["README.md"]
  s.homepage = "http://github.com/nex3/rb-inotify"
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "A Ruby wrapper for Linux's inotify, using FFI"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<ffi>, [">= 0.5.0"])
      s.add_development_dependency(%q<yard>, [">= 0.4.0"])
    else
      s.add_dependency(%q<ffi>, [">= 0.5.0"])
      s.add_dependency(%q<yard>, [">= 0.4.0"])
    end
  else
    s.add_dependency(%q<ffi>, [">= 0.5.0"])
    s.add_dependency(%q<yard>, [">= 0.4.0"])
  end
end

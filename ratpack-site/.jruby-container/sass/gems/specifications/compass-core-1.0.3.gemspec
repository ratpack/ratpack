# -*- encoding: utf-8 -*-
# stub: compass-core 1.0.3 ruby lib

Gem::Specification.new do |s|
  s.name = "compass-core"
  s.version = "1.0.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Chris Eppstein", "Scott Davis", "Eric M. Suzanne", "Brandon Mathis"]
  s.date = "2015-01-16"
  s.description = "The Compass core stylesheet library and minimum required ruby extensions. This library can be used stand-alone without the compass ruby configuration file or compass command line tools."
  s.email = ["chris@eppsteins.net"]
  s.homepage = "http://compass-style.org/reference/compass/"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "The Compass core stylesheet library"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<sass>, ["< 3.5", ">= 3.3.0"])
      s.add_runtime_dependency(%q<multi_json>, ["~> 1.0"])
      s.add_development_dependency(%q<bundler>, [">= 0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<sass>, ["< 3.5", ">= 3.3.0"])
      s.add_dependency(%q<multi_json>, ["~> 1.0"])
      s.add_dependency(%q<bundler>, [">= 0"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<sass>, ["< 3.5", ">= 3.3.0"])
    s.add_dependency(%q<multi_json>, ["~> 1.0"])
    s.add_dependency(%q<bundler>, [">= 0"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end

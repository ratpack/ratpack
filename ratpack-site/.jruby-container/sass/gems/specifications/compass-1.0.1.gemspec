# -*- encoding: utf-8 -*-
# stub: compass 1.0.1 ruby lib

Gem::Specification.new do |s|
  s.name = "compass"
  s.version = "1.0.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Chris Eppstein", "Scott Davis", "Eric M. Suzanne", "Brandon Mathis", "Nico Hagenburger"]
  s.date = "2014-08-19"
  s.description = "Compass is a Sass-based Stylesheet Framework that streamlines the creation and maintenance of CSS."
  s.email = "chris@eppsteins.net"
  s.executables = ["compass"]
  s.files = ["bin/compass"]
  s.homepage = "http://compass-style.org"
  s.post_install_message = "    Compass is charityware. If you love it, please donate on our behalf at http://umdf.org/compass Thanks!\n"
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "A Real Stylesheet Framework"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<sass>, ["< 3.5", ">= 3.3.13"])
      s.add_runtime_dependency(%q<compass-core>, ["~> 1.0.1"])
      s.add_runtime_dependency(%q<compass-import-once>, ["~> 1.0.5"])
      s.add_runtime_dependency(%q<chunky_png>, ["~> 1.2"])
      s.add_runtime_dependency(%q<rb-fsevent>, [">= 0.9.3"])
      s.add_runtime_dependency(%q<rb-inotify>, [">= 0.9"])
    else
      s.add_dependency(%q<sass>, ["< 3.5", ">= 3.3.13"])
      s.add_dependency(%q<compass-core>, ["~> 1.0.1"])
      s.add_dependency(%q<compass-import-once>, ["~> 1.0.5"])
      s.add_dependency(%q<chunky_png>, ["~> 1.2"])
      s.add_dependency(%q<rb-fsevent>, [">= 0.9.3"])
      s.add_dependency(%q<rb-inotify>, [">= 0.9"])
    end
  else
    s.add_dependency(%q<sass>, ["< 3.5", ">= 3.3.13"])
    s.add_dependency(%q<compass-core>, ["~> 1.0.1"])
    s.add_dependency(%q<compass-import-once>, ["~> 1.0.5"])
    s.add_dependency(%q<chunky_png>, ["~> 1.2"])
    s.add_dependency(%q<rb-fsevent>, [">= 0.9.3"])
    s.add_dependency(%q<rb-inotify>, [">= 0.9"])
  end
end

# -*- encoding: utf-8 -*-
# stub: chunky_png 1.3.4 ruby lib

Gem::Specification.new do |s|
  s.name = "chunky_png"
  s.version = "1.3.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Willem van Bergen"]
  s.date = "2015-02-16"
  s.description = "    This pure Ruby library can read and write PNG images without depending on an external\n    image library, like RMagick. It tries to be memory efficient and reasonably fast.\n\n    It supports reading and writing all PNG variants that are defined in the specification,\n    with one limitation: only 8-bit color depth is supported. It supports all transparency,\n    interlacing and filtering options the PNG specifications allows. It can also read and\n    write textual metadata from PNG files. Low-level read/write access to PNG chunks is\n    also possible.\n\n    This library supports simple drawing on the image canvas and simple operations like\n    alpha composition and cropping. Finally, it can import from and export to RMagick for\n    interoperability.\n\n    Also, have a look at OilyPNG at http://github.com/wvanbergen/oily_png. OilyPNG is a\n    drop in mixin module that implements some of the ChunkyPNG algorithms in C, which\n    provides a massive speed boost to encoding and decoding.\n"
  s.email = ["willem@railsdoctors.com"]
  s.extra_rdoc_files = ["README.rdoc", "BENCHMARKING.rdoc", "CONTRIBUTING.rdoc", "CHANGELOG.rdoc"]
  s.files = ["README.rdoc", "BENCHMARKING.rdoc", "CONTRIBUTING.rdoc", "CHANGELOG.rdoc"]
  s.homepage = "http://wiki.github.com/wvanbergen/chunky_png"
  s.licenses = ["MIT"]
  s.rdoc_options = ["--title", "chunky_png", "--main", "README.rdoc", "--line-numbers", "--inline-source"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "Pure ruby library for read/write, chunk-level access to PNG files"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<rspec>, ["~> 3"])
    else
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<rspec>, ["~> 3"])
    end
  else
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<rspec>, ["~> 3"])
  end
end

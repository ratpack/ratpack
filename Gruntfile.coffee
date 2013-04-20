# Generated on 2013-04-16 using generator-webapp 0.1.7
"use strict"
lrSnippet = require("grunt-contrib-livereload/lib/utils").livereloadSnippet
mountFolder = (connect, dir) ->
  connect.static require("path").resolve(dir)


# # Globbing
# for performance reasons we're only matching one level down:
# 'test/spec/{,*/}*.js'
# use this if you want to match all subfolders:
# 'test/spec/**/*.js'
module.exports = (grunt) ->

  # load all grunt tasks
  require("matchdep").filterDev("grunt-*").forEach grunt.loadNpmTasks

  # configurable paths
  yeomanConfig =
    src: "src/main/static"
    target: "src/ratpack/public"
    dist: "build/public" # todo: may need to be updated but ratpack gradle task not building right

  grunt.initConfig
    yeoman: yeomanConfig
    watch:
      coffee:
        files: ["<%= yeoman.src %>/scripts/{,*/}*.coffee"]
        tasks: ["coffee:build"]

      compass:
        files: ["<%= yeoman.src %>/styles/{,*/}*.{scss,sass}"]
        tasks: ["compass:build"]

    clean:
      all:
        files: [
          dot: true
          src: ["<%= yeoman.target %>/scripts", "<%= yeoman.target %>/styles", "<%= yeoman.dist %>/*", "!<%= yeoman.dist %>/.git*"]
        ]

    jshint:
      options:
        jshintrc: ".jshintrc"

      all: ["<%= yeoman.src %>/scripts/{,*/}*.js", "!<%= yeoman.src %>/lib/*"]

    coffee:
      build:
        files: [
          expand: true
          cwd: "<%= yeoman.src %>/scripts"
          src: "{,*/}*.coffee"
          dest: "<%= yeoman.target %>/scripts"
          ext: ".js"
        ]

    compass:
      options:
        sassDir: "<%= yeoman.src %>/styles"
        cssDir: "<%= yeoman.target %>/styles"
        imagesDir: "<%= yeoman.target %>/images"
        javascriptsDir: "<%= yeoman.src %>/scripts"
        fontsDir: "styles/fonts"
        importPath: "<%= yeoman.src %>/lib"
        relativeAssets: true

      dist: {}
      build:
        options:
          debugInfo: true

    rev:
      dist:
        files:
          src: [
            "<%= yeoman.dist %>/scripts/{,*/}*.js"
            "<%= yeoman.dist %>/styles/{,*/}*.css"
            "<%= yeoman.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp}"
            "<%= yeoman.dist %>/styles/fonts/*"
          ]

    useminPrepare:
      html: "<%= yeoman.src %>/index.html"
      options:
        dest: "<%= yeoman.dist %>"

    usemin:
      html: ["<%= yeoman.dist %>/{,*/}*.html"]
      css: ["<%= yeoman.dist %>/styles/{,*/}*.css"]
      options:
        dirs: ["<%= yeoman.dist %>"]

    imagemin:
      dist:
        files: [
          expand: true
          cwd: "<%= yeoman.target %>/images"
          src: "{,*/}*.{png,jpg,jpeg}"
          dest: "<%= yeoman.dist %>/images"
        ]

    svgmin:
      dist:
        files: [
          expand: true
          cwd: "<%= yeoman.target %>/images"
          src: "{,*/}*.svg"
          dest: "<%= yeoman.dist %>/images"
        ]

    cssmin:
      dist:
        files:
          "<%= yeoman.dist %>/styles/ratpack.css": ["<%= yeoman.target %>/styles/{,*/}ratpack.css", "<%= yeoman.src %>/styles/{,*/}ratpack.css"]
          "<%= yeoman.dist %>/styles/logo.css": ["<%= yeoman.target %>/styles/{,*/}logo.css", "<%= yeoman.src %>/styles/{,*/}logo.css"]

    htmlmin:
      dist:
        options: {}
        files: [
          expand: true
          cwd: "<%= yeoman.src %>"
          src: "*.html"
          dest: "<%= yeoman.dist %>"
        ]


    # Put files not handled in other tasks here
    copy:
      dist:
        files: [
          expand: true
          dot: true
          cwd: "<%= yeoman.target %>"
          dest: "<%= yeoman.dist %>"
          src: ["*.{ico,txt}", ".htaccess", "images/{,*/}*.{webp,gif}", "styles/fonts/*"]
        ]

    concurrent:
      develop: ["coffee:build", "compass:build"]
      dist: ["coffee", "compass:dist", "imagemin", "svgmin", "htmlmin"]

  grunt.renameTask "regarde", "watch"
  grunt.registerTask "develop", ["concurrent:develop", "watch"]
  grunt.registerTask "build", ["clean", "useminPrepare", "concurrent:dist", "cssmin", "concat", "uglify", "copy", "rev", "usemin"]
  grunt.registerTask "default", ["jshint", "build"]

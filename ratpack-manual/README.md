# The Ratpack Manual

The manual is the canonical documentation for Ratpack.
It is both book like documentation _and_ the API reference (i.e. Javadoc).
It is the documentation available @ http://www.ratpack.io/manual/current.

## Contributions, ideas and suggestions

If you're looking to contribute to the Ratpack project, improving the documentation greatly helps everyone and is a great way to increase your own Ratpack knowledge.
We've tried to make working on the documentation easy.

If you'd like to contribute the documentation, have an idea for some content to add or improvement suggestions,
_please_ open an issue ticket @ [github.com/ratpack/ratpack/issues](https://github.com/ratpack/ratpack/issues).

We will be extremely grateful for any documentation work and will work with you to get the changes in.

## How it works

### Javadoc

The build for this project defines a javadoc task that runs the Javadoc tool on the entire public API of all components.
Custom CSS and custom Javascript are injected into the pages to make the look and feel consistent with the manual pages.
The output javadoc is eventually made available as the `api` directory inside the final manual output.

### Manual chapters

The manual chapters are written as [Markdown](https://daringfireball.net/projects/markdown/) files, in the `src/content/chapters` dir.
Each file is prefixed with a two digit number to determine the ordering.
When compiling the manual, these files are _joined_ in order, then run through a Markdown processor to create one giant HTML document.
The document is then broken into chapters based on the `<h1>` tags.
Therefore, each `<h1>` effectively defines a chapter.
By convention, we have one chapter per file so the first thing in each chapter file is the Markdown equivalent of a `<h1>` (i.e. `# «title»`)
For more information on the processing, see the Gradle build file.

### Building & viewing the manual

No external software is required; the build is self contained.

From the root of the project:

* `./gradlew openManual` builds the manual (chapters and API reference) and opens it in your default browser
* `./gradlew packageManual` rebuilds but doesn't open another browser window (use this if you already have the manual open)

The edit process is usually:

1. run `./gradlew openManual` to build it and open it for the first time
1. edit/add Javadoc or manual chapters
1. run `./gradlew packageManual` to rebuild the manual
1. refresh your browser

Note: instead of running `./gradlew packageManual`, you can abbreviate it to `./gradlew pM`.

### Building on Windows

Note that the site makes use of SASS which means we're making use of Ruby via JRuby.
JRuby has poor Windows support and requires that the project is built from a terminal as an administrator.

Refer to https://github.com/jruby/jruby/issues/1219 for details.

## Guide to writing content

### Goals

The documentation (i.e. manual and API reference) shall…

1. help new users get started quickly with Ratpack;
1. provide experienced Ratpack users with answers to specific questions;
1. be concise, effective, non hyperbolic, yet not boring;
1. be correct and accurate at each version;
1. contain exclusively automatically test code samples that are correct and accurate;
1. not duplicate information that is available elsewhere on the Internet.
1. introduce topics and content through the manual pages and linking through to the API reference for detail
1. the API reference (Javadoc) should be complete, useful, and contain tested examples

### Structure and purpose of chapters vs. API reference

The manual encompasses the chapters and the API reference.
This begs the question: “what type of information goes where”?

* The _chapters_ introduce and explain concepts and capabilities - it is generally the starting point
* The _API reference_ contains the detail and technical information - it is generally the most useful once you are already using Ratpack

The _chapters_ can be thought of as sitting atop the API reference.
As such, it liberally links through to the API reference where the user can go for further detail.
There will by necessity be some duplication of information between the two sources, but this should be minimised where possible.

The _chapters_ should be at a reasonably high level and introduce topics and concepts.
The _API reference_ should contain the majority of the “how to” information.

### API reference

The API reference content is standard Javadoc, extracted from the Javadoc comments of the public API of all components.
To add content simply work with these comments on the source.

#### Style guide

1. All `protected` and higher level visibility elements (methods, classes etc.) must be documented
1. Make liberal use of `{@link}` and `@see` to guide the user through the docs
1. API reference docs should attempt to be self contained. That is, they should not rely on manual content.

### Chapters

#### Writing style

* Each sentence should begin on a new line (this makes diffs _much_ easier to read).
* Headings should be in title case, e.g. "Unit testing handlers" rather than "Unit Testing Handlers".
* Proper names should be capitalized, e.g. "You can test Ratpack with Groovy and Spock" rather than "You can test ratpack with groovy and spock".
* File or directory names and literal URLs should be in code blocks (via \`\`), e.g. "The handler renders a template called `index.html`", "Point your browser at `http://localhost:5050/`".
* Library names should be in code blocks, e.g. "Groovy support is included in the `ratpack-groovy` package".
* Class, variable and method names should be rendered as code, e.g. "The `Handler` interface has a single method – `handle(Context)`".
* Literal values such as `true`, `false`, `null` or `"a string"` should be code blocks.
* Multi line code blocks should be written as [fenced code blocks](https://help.github.com/articles/github-flavored-markdown#fenced-code-blocks)
* To support syntax highlighting code blocks should, where appropriate, include a `language-xxx` immediately following the opening back-ticks where `xxx` is the language of the sample (use `language-bash` for terminal commands`).
* When discussing classes from the Ratpack API you should make the first reference to the class name a link to the Javadoc API page for that class. Only link the first reference in a chapter not all of them.
* When talking about common concepts such as Ratpack handlers it's not always necessary to use the class name – too many code blocks can make the text harder to read. e.g. "You can unit test your handler with `InvocationBuilder`" rather than "You can unit test your `Handler` with `InvocationBuilder`.
* The first paragraph of each chapter should sufficiently introduce the content of the rest of the chapter so the user can determine whether they need to read it
* The first paragraph of each chapter should make any prerequisite chapters clear

#### Chapter numbering

To limit the reshuffling needed when introducing or reordering chapters, there are defined ranges for chapter numbers:

* `01..09` - introductory
* `10..29` - core in depth (incl. testing)
* `30..59` - integrations & modules (e.g. jackson, metrics, thymeleaf)
* `60..69` - build time
* `70..89` - deployment (e.g. heroku)
* `90..99` - meta (about the project or manual)

### Tested code snippets

One of the most challenging aspects of writing the documentation is working out how code snippets can be written so they can be tested.
The build provided generally machinery for executing the tests, but some creativity is sometimes required when crafting the sample so that it is meaningful.

#### Mechanics

The `ratpack-manual` project has Groovy tests in `src/test/groovy` that test the code snippets in both the Javadoc and chapters.

* See [JavadocCodeSnippetTests](https://github.com/ratpack/ratpack/blob/master/ratpack-manual/src/test/groovy/ratpack/manual/JavadocCodeSnippetTests.groovy)
* See [ManualCodeSnippetTests](https://github.com/ratpack/ratpack/blob/master/ratpack-manual/src/test/groovy/ratpack/manual/ManualCodeSnippetTests.groovy)

Both tests do basically the same thing:

1. Extract out all of the code snippets from the source (i.e. javadoc comments on source files, markdown chapter files)
2. Extract the snippet type of each code snippet (declared on the snippet)
3. Execute each snippet as statically compiled Groovy, using the fixture for the snippet type

Both of these tests can be run within IDEA, iteratively while writing code snippets.

Chapter code snippets are written as fenced code blocks…

    ```language-groovy groovy-chain-dsl
    «code snippet»
    ```

API reference code snippets are written as `<pre>` tags

    /**
     * Some description here.
     *
     * <pre class="groovy-chain-dsl">
     * «code snippet»
     * </pre>
     */

The code is simply executed.
Because it is statically compiled, it guarantees type and symbol name correctness.
You may wish to include `assert` statements to verify logical correctness.

#### Fixtures

Each snippet type has an associated fixture.
This allows implicit code to be prepended and/or appended to the snippet.
It also allows pre-flight and post-flight code to be run.

See the `FIXTURES` constant of [JavadocCodeSnippetTests](https://github.com/ratpack/ratpack/blob/master/ratpack-manual/src/test/groovy/ratpack/manual/JavadocCodeSnippetTests.groovy)
and [ManualCodeSnippetTests](https://github.com/ratpack/ratpack/blob/master/ratpack-manual/src/test/groovy/ratpack/manual/ManualCodeSnippetTests.groovy) for the mapping of snippet types to fixture implementation.

layout 'layout.gtpl', true,
bodyContents: contents {
  section(id: "main") {
    article(class: "content") {
      h1("Articles, example apps and learning resources.")

      h3 {
        a(href: "http://www.infoq.com/articles/Ratpack-and-Spring-Boot", "Build High Performance JVM Microservices with Ratpack & Spring Boot")
      }
      p "Posted by Dan Woods on Jul 28, 2015 @ InfoQ."
      p {
        yield "This articles describes how you can use Ratpack in conjunction with "
        a(href: "http://projects.spring.io/spring-boot/", "Spring Boot")
        yield " for fun and profit."
      }

      h3 {
        a href: "https://www.voxxed.com/blog/2015/08/ratpack-the-java-8-web-framework-for-independent-thinkers/", "Ratpack: A Java 8 Web Framework for Independent Thinkers"
      }
      p "This article is an interview with Ratpack creator Luke Daley just prior to the 1.0 release on August 14th, 2015."
    }
  }
}

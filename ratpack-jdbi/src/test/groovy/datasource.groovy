dataSource {
  url = "jdbc:h2:mem:jdbi"
  driverClassName = "org.h2.Driver"
  username = "sa"
  password = ""

  pool {
    maxWait = 60000
    maxIdle = 5
    maxActive = 8
  }

}

dataSources {

  prod {
    url = "jdbc:h2:mem:jdbi-prod"
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""

    pool {
      maxWait = 60000
      maxIdle = 5
      maxActive = 8
    }


  }


}

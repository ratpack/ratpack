# ratpack-rx2
Ratpack support for rxjava2

[![Build Status](https://travis-ci.org/drmaas/ratpack-rx2.svg?branch=master)](https://travis-ci.org/drmaas/ratpack-rx2)

[![forthebadge](https://forthebadge.com/images/badges/uses-badges.svg)](https://forthebadge.com)

## Gradle
```
compile 'me.drmaas:ratpack-rx2:x.x.x'
```

## Maven
```
<dependency>
    <groupId>me.drmaas</groupId>
    <artifactId>ratpack-rx2</artifactId>
    <version>x.x.x/version>
</dependency>
```

## Examples

### Observable
```
ratpack {
  handlers {
    get(":value") {
      observe(Blocking.get {
        pathTokens.value
      }) subscribe {
        render it
      }
    }
  }
}
```

### Flowable
```
ratpack {
  handlers {
    get(":value") {
      flow(Blocking.get {
        pathTokens.value
      }, BackpressureStrategy.BUFFER) subscribe {
        render it
      }
    }
  }
}
```

A detailed listing of use cases should follow logically from those documentated at https://ratpack.io/manual/current/api/ratpack/rx/RxRatpack.html

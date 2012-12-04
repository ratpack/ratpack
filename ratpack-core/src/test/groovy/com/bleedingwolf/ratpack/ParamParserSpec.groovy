package com.bleedingwolf.ratpack

import org.junit.Test

import spock.lang.Specification
import com.bleedingwolf.ratpack.internal.ParamParser

class ParamParserSpec extends Specification {

  Map<String, ?> params(String input) {
    new ParamParser().parse(input)
  }

  void simpleValuePairs() {
    expect:
    params("key=val&key2=val2") == [key: "val", key2: "val2"]
  }

  void arrayWithOneEntry() {
    expect:
    params("key[]=val") == [key: ["val"]]
  }

  void arrayWithTwoEntries() {
    expect:
    params("key[]=val1&key[]=val2") == [key: ["val1", "val2"]]
  }

  void map() {
    expect:
    params("key[a]=val1&key[b]=val2") == [key: [a: "val1", "b": "val2"]]
  }

  void nestedMap() {
    expect:
    params("key[a][a]=val1&key[a][b]=val2") == [key: [a: [a: "val1", b: "val2"]]]
  }

  void arrayWithinNestedMap() {
    expect:
    params("key[a][b][]=val1&key[a][b][]=val2") == [key: [a: [b: ["val1", "val2"]]]]
  }

  void mapWithinArrayDifferentKeyAddsToSameMap() {
    expect:
    params("a[]a=val1&a[]b=val2") == [a: [[a: "val1", b: "val2"]]]
  }

  void mapWithinArraySameKeyGetsNewMap() {
    expect:
    params("a[]a=val1&a[]a=val2") == [a: [[a: "val1"], [a: "val2"]]]
  }

}

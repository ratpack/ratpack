/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.jdbctx.internal;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assert;
import ratpack.exec.Blocking;
import ratpack.exec.Operation;
import ratpack.func.Block;
import ratpack.jdbctx.Transaction;
import ratpack.test.exec.ExecHarness;
import ratpack.util.Exceptions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Example {

  private static DataSource txDs;
  private static Transaction tx;

  public static void main(String[] args) throws Exception {
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:transactionExamples;DB_CLOSE_DELAY=-1");

    txDs = Transaction.dataSource(ds);
    tx = Transaction.create(ds::getConnection);

    try (Connection connection = txDs.getConnection()) {
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate("CREATE TABLE tbl (value VARCHAR(50)) ");
      }
    }

    List<Block> examples = Arrays.asList(
      Example::singleTransactionExample,
      Example::singleTransactionRollbackExample,
      Example::nestedTransactionExample,
      Example::nestedTransactionRollbackExample,
      () -> manualTransactionExample(true),
      () -> manualTransactionExample(false)
    );

    try (ExecHarness harness = ExecHarness.harness()) {
      for (Block example : examples) {
        harness.execute(Operation.of(example));
        reset();
      }
    }

  }

  private static void reset() throws SQLException {
    try (Connection connection = txDs.getConnection()) {
      connection.createStatement().execute("DELETE FROM tbl");
    }
  }

  private static Operation insert(String value) {
    return Blocking.op(() -> {
      try (Connection connection = txDs.getConnection()) {
        connection.createStatement().execute("INSERT INTO tbl (value) VALUES (" + value + ")");
      }
    });
  }

  private static Block assertValues(String... expected) {
    return () ->
      Blocking.get(() -> {
        try (Connection connection = txDs.getConnection()) {
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT value FROM tbl;");
          List<String> actual = new ArrayList<>();
          while (resultSet.next()) {
            actual.add(resultSet.getString(1));
          }
          return actual;
        }
      })
        .then(actual -> Assert.assertEquals(Arrays.asList(expected), actual));
  }

  // BEGIN EXAMPLES

  private static void singleTransactionExample() {
    tx.wrap(insert("1")).then(assertValues("1"));
  }

  private static void singleTransactionRollbackExample() {
    RuntimeException exception = new RuntimeException("1");
    tx.wrap(
      insert("1")
        .next(() -> {
          throw exception;
        })
    )
      .onError(e -> {
        Assert.assertSame(e, exception);
        Operation.of(assertValues()).then();
      })
      .then(() -> {
        throw new IllegalStateException("operation should have failed");
      });
  }

  private static void nestedTransactionExample() {
    tx.wrap(
      insert("1")
        .next(tx.wrap(insert("2")))
    )
      .then(assertValues("1", "2"));
  }

  private static void nestedTransactionRollbackExample() {
    RuntimeException exception = new RuntimeException("1");
    tx.wrap(
      insert("1")
        .next(
          tx.wrap(
            insert("2")
              .next(() -> {
                throw exception;
              })
          )
            // recover from the error, and insert something else
            .mapError(e -> insert("3").then())
        )
    )
      .then(assertValues("1", "3"));
  }

  private static void manualTransactionExample(boolean fail) {
    tx.begin()
      .next(insert("1"))
      .next(() -> {
        if (fail) {
          throw new RuntimeException("!");
        }
      })
      .onError(e ->
        tx.rollback().then(() -> {
          throw Exceptions.toException(e);
        })
      )
      .next(tx.commit())
      .onError(e -> assertValues().map(Operation::of).then())
      .then(assertValues("1"));
  }
}

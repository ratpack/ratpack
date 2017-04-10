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

package ratpack.jdbctx;

import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Factory;
import ratpack.jdbctx.internal.BoundTransaction;
import ratpack.jdbctx.internal.DefaultTransaction;
import ratpack.jdbctx.internal.TransactionalDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Optional;

/**
 * A JDBC transaction coordinator.
 * <p>
 * An instance of this type represents a <i>potential</i> transaction or an active transaction at any given time.
 * The {@link #begin()} method must be called on a transaction to actually initiate a transaction.
 * <p>
 * This type is effectively an asynchronous adapter to the JDBC {@link Connection} class's transactional methods such as
 * {@link Connection#commit()}, {@link Connection#setSavepoint()}, {@link Connection#rollback()} etc.
 * <p>
 * It also (optionally) manages an execution global binding, analogous to thread local globals with synchronous frameworks such as Spring's transaction management.
 * This allows implicit use of the “current” transaction's connection (see {@link #current()} and {@link #connection()}).
 * <p>
 * Transaction objects are reusable, but cannot be used concurrently.
 *
 * <pre class="java">{@code
 * import org.h2.jdbcx.JdbcDataSource;
 * import org.junit.Assert;
 * import ratpack.exec.Blocking;
 * import ratpack.exec.Operation;
 * import ratpack.func.Block;
 * import ratpack.jdbctx.Transaction;
 * import ratpack.test.exec.ExecHarness;
 * import ratpack.util.Exceptions;
 *
 * import javax.sql.DataSource;
 * import java.sql.Connection;
 * import java.sql.ResultSet;
 * import java.sql.SQLException;
 * import java.sql.Statement;
 * import java.util.ArrayList;
 * import java.util.Arrays;
 * import java.util.List;
 *
 * public class Example {
 *
 *   private static DataSource txDs;
 *   private static Transaction tx;
 *
 *   public static void main(String[] args) throws Exception {
 *     JdbcDataSource ds = new JdbcDataSource();
 *     ds.setURL("jdbc:h2:mem:transactionExamples;DB_CLOSE_DELAY=-1");
 *
 *     txDs = Transaction.dataSource(ds);
 *     tx = Transaction.create(ds::getConnection);
 *
 *     try (Connection connection = txDs.getConnection()) {
 *       try (Statement statement = connection.createStatement()) {
 *         statement.executeUpdate("CREATE TABLE tbl (value VARCHAR(50)) ");
 *       }
 *     }
 *
 *     List<Block> examples = Arrays.asList(
 *       Example::singleTransactionExample,
 *       Example::singleTransactionRollbackExample,
 *       Example::nestedTransactionExample,
 *       Example::nestedTransactionRollbackExample,
 *       () -> manualTransactionExample(true),
 *       () -> manualTransactionExample(false)
 *     );
 *
 *     try (ExecHarness harness = ExecHarness.harness()) {
 *       for (Block example : examples) {
 *         harness.execute(Operation.of(example));
 *         reset();
 *       }
 *     }
 *
 *   }
 *
 *   private static void reset() throws SQLException {
 *     try (Connection connection = txDs.getConnection()) {
 *       connection.createStatement().execute("DELETE FROM tbl");
 *     }
 *   }
 *
 *   private static Operation insert(String value) {
 *     return Blocking.op(() -> {
 *       try (Connection connection = txDs.getConnection()) {
 *         connection.createStatement().execute("INSERT INTO tbl (value) VALUES (" + value + ")");
 *       }
 *     });
 *   }
 *
 *   private static Block assertValues(String... expected) {
 *     return () ->
 *       Blocking.get(() -> {
 *         try (Connection connection = txDs.getConnection()) {
 *           Statement statement = connection.createStatement();
 *           ResultSet resultSet = statement.executeQuery("SELECT value FROM tbl;");
 *           List<String> actual = new ArrayList<>();
 *           while (resultSet.next()) {
 *             actual.add(resultSet.getString(1));
 *           }
 *           return actual;
 *         }
 *       })
 *         .then(actual -> Assert.assertEquals(Arrays.asList(expected), actual));
 *   }
 *
 *   // BEGIN EXAMPLES
 *
 *   private static void singleTransactionExample() {
 *     tx.wrap(insert("1")).then(assertValues("1"));
 *   }
 *
 *   private static void singleTransactionRollbackExample() {
 *     RuntimeException exception = new RuntimeException("1");
 *     tx.wrap(
 *       insert("1")
 *         .next(() -> {
 *           throw exception;
 *         })
 *     )
 *       .onError(e -> {
 *         Assert.assertSame(e, exception);
 *         Operation.of(assertValues()).then();
 *       })
 *       .then(() -> {
 *         throw new IllegalStateException("operation should have failed");
 *       });
 *   }
 *
 *   private static void nestedTransactionExample() {
 *     tx.wrap(
 *       insert("1")
 *         .next(tx.wrap(insert("2")))
 *     )
 *       .then(assertValues("1", "2"));
 *   }
 *
 *   private static void nestedTransactionRollbackExample() {
 *     RuntimeException exception = new RuntimeException("1");
 *     tx.wrap(
 *       insert("1")
 *         .next(
 *           tx.wrap(
 *             insert("2")
 *               .next(() -> {
 *                 throw exception;
 *               })
 *           )
 *             // recover from the error, and insert something else
 *             .mapError(e -> insert("3").then())
 *         )
 *     )
 *       .then(assertValues("1", "3"));
 *   }
 *
 *   private static void manualTransactionExample(boolean fail) {
 *     tx.begin()
 *       .next(insert("1"))
 *       .next(() -> {
 *         if (fail) {
 *           throw new RuntimeException("!");
 *         }
 *       })
 *       .onError(e ->
 *         tx.rollback().then(() -> {
 *           throw Exceptions.toException(e);
 *         })
 *       )
 *       .next(tx.commit())
 *       .onError(e -> assertValues().map(Operation::of).then())
 *       .then(assertValues("1"));
 *   }
 * }
 * }</pre>
 *
 * @since 1.5
 */
public interface Transaction {

  /**
   * Decorates the given data source to be {@link Transaction} aware.
   * <p>
   * This method can be used to create a data source that implicitly uses the connection of the active transaction if there is one.
   * This is a typical pattern in an application that interacts with a single database.
   * More complex applications may require more explicit connection assignment.
   * <p>
   * If a connection is requested while there is a current active transaction, its connection will be returned.
   * The returned connection is effectively un-closeable.
   * It will be closed when the overarching transaction is completed.
   * <p>
   * If a connection is requested while there is NOT a current active transaction, a connection from {@code dataSource} will be returned.
   * <p>
   * All other methods/functions always delegate to the given {@code dataSource}.
   *
   * @param dataSource the data source to delegate to
   * @return a connection aware data source
   */
  static DataSource dataSource(DataSource dataSource) {
    return new TransactionalDataSource(dataSource);
  }

  /**
   * The current execution bound transaction, if any.
   * <p>
   * When a transaction is active (i.e. {@link #begin()} has been called), the instance is bound to the current execution.
   * This behaviour can be disabled via {@link #autoBind(boolean)}.
   *
   * @return the current execution bound transaction, if any.
   */
  static Optional<Transaction> current() {
    return Execution.currentOpt().flatMap(e -> e.maybeGet(Transaction.class));
  }

  /**
   * The connection of the current transaction if it is active.
   *
   * @return the connection of the current transaction if it is active
   */
  static Optional<Connection> connection() {
    return current().flatMap(Transaction::getConnection);
  }

  /**
   * Creates a new transaction.
   * <p>
   * This method always creates a new transaction.
   * It is more typical to use {@link #get(Factory)} to use the existing transaction, or create a new one if none exists.
   *
   * @param connectionFactory the connection factory
   * @return the newly created transaction
   */
  static Transaction create(Factory<? extends Connection> connectionFactory) {
    return new DefaultTransaction(connectionFactory);
  }

  /**
   * Returns the current transaction if present, otherwise a newly created transaction.
   *
   * @param connectionFactory the connection factory
   * @return the current transaction if present, otherwise a newly created transaction
   */
  static Transaction get(Factory<? extends Connection> connectionFactory) {
    Optional<Transaction> current = current();
    if (current.isPresent()) {
      return current.get();
    } else {
      return create(connectionFactory);
    }
  }

  /**
   * Creates a transaction implementation that delegates to the execution bound transaction.
   * <p>
   * This transaction can be used as an application wide singleton.
   * When any transaction method is called,
   * it will delegate to the {@link #current()} transaction if there is one,
   * or it will {@link #create(Factory)} a new one.
   * <p>
   * This differs to {@link #get(Factory)} in that this method returns a dynamically delegating transaction,
   * instead of an actual transaction.
   * <p>
   * Typically, this method can be used to create a single {@code Transaction} object that is used throughout the application.
   *
   * @param connectionFactory the connection factory
   * @return a transaction object that delegates to the current transaction, or creates a new one, for each method
   */
  static Transaction bound(Factory<? extends Connection> connectionFactory) {
    return new BoundTransaction(connectionFactory);
  }

  /**
   * Returns the current transaction if present
   *
   * @return the current transaction if present
   * @throws TransactionException if there is no bound transaction
   */
  static Transaction join() throws TransactionException {
    return current().orElseThrow(() -> new TransactionException("There is no bound transaction to join"));
  }

  /**
   * Binds this transaction to the current execution.
   * <p>
   * The instance is added to the current execution's registry.
   * <p>
   * It is typically not necessary to call this directly.
   * Transactions default to “auto binding”.
   * That is, this method is called implicitly when the transaction starts.
   *
   * @return {@code this}
   * @throws TransactionException if a different transaction is bound to the execution
   */
  default Transaction bind() throws TransactionException {
    Execution execution = Execution.current();
    execution.maybeGet(Transaction.class).ifPresent(t -> {
      if (t != this) {
        throw new TransactionException("A transaction is already bound");
      }
    });
    execution.add(Transaction.class, this);
    return this;
  }

  /**
   * Unbinds this transaction from the current execution.
   * <p>
   * If the transaction is not bound, this method is effectively a noop and returns false.
   *
   * @return whether this transaction was actually bound
   * @throws TransactionException if a different transaction is bound to the execution
   * @see #bind()
   */
  default boolean unbind() {
    Execution execution = Execution.current();
    Optional<Transaction> transaction = execution.maybeGet(Transaction.class);
    if (transaction.isPresent() && transaction.get() == this) {
      execution.remove(Transaction.class);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Whether this transaction is active.
   *
   * @return whether this transaction is active
   */
  default boolean isActive() {
    return getConnection().isPresent();
  }

  /**
   * The underlying connection for the transaction.
   * <p>
   * The optional will be empty if the transaction is not active.
   *
   * @return the underlying connection for the transaction
   */
  Optional<Connection> getConnection();

  /**
   * Starts a transaction.
   * <p>
   * If the transaction is not active, a new connection will be acquired.
   * If a transaction has already begun, creates a new savepoint and adds it to an internal stack.
   * <p>
   * A call to this method MUST be paired with a subsequent call to either {@link #commit()} or {@link #rollback()}.
   * Generally, it is more convenient to use {@link #wrap(Promise)} or {@link #wrap(Operation)} which manages this.
   *
   * @return an operation to start a transaction or create a savepoint
   */
  Operation begin();

  /**
   * Initiates a transaction rollback.
   * <p>
   * If the transaction is not active, the operation will fail with {@link TransactionException}.
   * <p>
   * If the save point stack is empty (i.e. there are no nested transactions),
   * A {@link Connection#rollback()} is issued and the underlying connection is closed.
   * The transaction will no longer be active and will be {@link #unbind() unbound} (if auto-binding).
   * <p>
   * If the save point stack is NOT empty, the most recent savepoint is rolled back.
   * The underlying connection will not be closed, and the transaction will remain active and {@link #bind() bound} (if auto-binding).
   *
   * @return an operation that rolls back the transaction or to the most recent savepoint
   */
  Operation rollback();

  /**
   * Commits the transaction, or pops the most recent savepoint off the stack.
   * <p>
   * If the transaction is not active, the operation will fail with {@link TransactionException}.
   * <p>
   * If the save point stack is empty (i.e. there are no nested transactions),
   * A {@link Connection#commit()} ()} is issued and the underlying connection is closed.
   * The transaction will no longer be active and will be {@link #unbind() unbound} (if auto-binding).
   * <p>
   * If the save point stack is NOT empty, the most recent savepoint is popped from the stack.
   * The underlying connection will not be closed, and the transaction will remain active and {@link #bind() bound} (if auto-binding).
   *
   * @return an operation that commits the transaction or pops the most recent save point
   */
  Operation commit();

  /**
   * Sets the auto binding behaviour of the transaction.
   * <p>
   * An auto-binding transaction will implicitly call {@link #bind()} when becoming active,
   * and {@link #unbind()} when closing.
   * <p>
   * It generally only helps to disable auto binding if multiple connections are used within the same execution.
   * <p>
   * Defaults to {@code true}.
   *
   * @param autoBind whether to enable auto-binding
   * @return {@code this}
   */
  Transaction autoBind(boolean autoBind);

  /**
   * Whether this transaction is auto-binding.
   * @return whether this transaction is auto-binding
   */
  boolean isAutoBind();

  /**
   * Decorates the given promise in a transaction boundary.
   * <p>
   * The decoration effectively calls {@link #begin()} before yielding the promise.
   * If it fails, {@link #rollback()} will be issued.
   * If it succeeds, {@link #commit()} will be issued.
   *
   * @param promise the promise to yield in a transaction
   * @param <T> the type of promised value
   * @return a promise that will yield within a transaction
   */
  <T> Promise<T> wrap(Promise<T> promise);

  /**
   * Executes the given factory and yields the resultant promise in a transaction.
   *
   * @param promiseFactory the factory of the promise to yield in a transaction
   * @param <T> the type of promised value
   * @return a promise that will yield within a transaction
   */
  default <T> Promise<T> wrap(Factory<? extends Promise<T>> promiseFactory) {
    return wrap(Promise.flatten(promiseFactory));
  }

  /**
   * Decorates the given operation in a transaction boundary.
   * <p>
   * The decoration effectively calls {@link #begin()} before yielding the operation.
   * If it fails, {@link #rollback()} will be issued.
   * If it succeeds, {@link #commit()} will be issued.
   *
   * @param operation the operation to yield in a transaction
   * @return a operation that will yield within a transaction
   */
  Operation wrap(Operation operation);

}

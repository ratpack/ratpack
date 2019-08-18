/*
 * Copyright 2017 the original author or authors.
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

import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Factory;
import ratpack.jdbctx.Transaction;
import ratpack.jdbctx.TransactionException;

import java.sql.Connection;
import java.util.Optional;

public class BoundTransaction implements Transaction {

  private final Factory<? extends Connection> connectionFactory;

  public BoundTransaction(Factory<? extends Connection> connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  private Transaction get() {
    return Transaction.get(connectionFactory);
  }

  @Override
  public Transaction bind() throws TransactionException {
    get().bind();
    return this;
  }

  @Override
  public boolean unbind() {
    return get().unbind();
  }

  @Override
  public boolean isActive() {
    return get().isActive();
  }

  @Override
  public Optional<Connection> getConnection() {
    return get().getConnection();
  }

  @Override
  public Operation begin() {
    return Operation.flatten(() -> get().begin());
  }

  @Override
  public Operation rollback() {
    return Operation.flatten(() -> get().rollback());
  }

  @Override
  public Operation commit() {
    return Operation.flatten(() -> get().commit());
  }

  @Override
  public Transaction autoBind(boolean autoBind) {
    get().autoBind(autoBind);
    return this;
  }

  @Override
  public boolean isAutoBind() {
    return get().isAutoBind();
  }

  @Override
  public <T> Promise<T> wrap(Promise<T> promise) {
    return Promise.flatten(() -> get().wrap(promise));
  }

  @Override
  public Operation wrap(Operation operation) {
    return Operation.of(() -> get().wrap(operation).then());
  }

}

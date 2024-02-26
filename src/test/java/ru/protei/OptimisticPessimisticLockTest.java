package ru.protei;

import com.mysql.cj.jdbc.Driver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;

public class OptimisticPessimisticLockTest {
    @BeforeAll
    public static void beforeAll() throws SQLException {
        DriverManager.registerDriver(new Driver());
    }

    @Test
    public void testOptimisticLock() throws SQLException {
        int countOfThreads = 30;

        withTable(() -> {
            withNewConnection(connection -> {
                Statement statement = connection.createStatement();
                statement.executeUpdate("insert into optimistic_pessimistic_lock (id, name, version) values (1, '0', 0)");
            });

            ExecutorService executorService = Executors.newFixedThreadPool(countOfThreads);
            List<Future<Boolean>> futures = new ArrayList<>(countOfThreads);

            for (int i = 0; i < countOfThreads; i++) {
                futures.add(executorService.submit(() -> {
                    withNewConnection(connection -> {
                        long updatedRows = 0;
                        int countOfTries = 1;

                        while (updatedRows < 1) {
                            System.out.println("Thread: " + Thread.currentThread().getName() + ". Try " + countOfTries++);
                            updatedRows = updateWithoutLock(connection);
                        }
                    }, TRANSACTION_READ_COMMITTED); // работает на любом уровне изолированности

                    return true;
                }));
            }

            futures.forEach(this::await);

            withNewConnection(connection -> Assertions.assertEquals(countOfThreads, getVersion(connection)));
        });
    }

    @Test
    public void testPessimisticLock() throws SQLException {
        int countOfThreads = 30;

        withTable(() -> {
            withNewConnection(connection -> {
                Statement statement = connection.createStatement();
                statement.executeUpdate("insert into optimistic_pessimistic_lock (id, name, version) values (1, '0', 0)");
            });

            ExecutorService executorService = Executors.newFixedThreadPool(countOfThreads);
            List<Future<Boolean>> futures = new ArrayList<>(countOfThreads);

            for (int i = 0; i < countOfThreads; i++) {
                futures.add(executorService.submit(() -> {
                    withNewConnection(this::updateWithLock, TRANSACTION_READ_COMMITTED, false); // работает на любом уровне изолированности

                    return true;
                }));
            }

            futures.forEach(this::await);

            withNewConnection(connection -> Assertions.assertEquals(countOfThreads, getVersion(connection)));
        });
    }

    private void await(Future<Boolean> future) {
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private long getVersion(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select version from optimistic_pessimistic_lock where id = 1");
        resultSet.next();
        return resultSet.getLong("version");
    }

    private int updateWithoutLock(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select * from optimistic_pessimistic_lock where id = 1");
        resultSet.next();
        long version = resultSet.getLong("version");
        long newVersion = version + 1;
        String newName = String.valueOf(newVersion);

        return connection.createStatement().executeUpdate("update optimistic_pessimistic_lock set name = " + newName + ", version = " + newVersion +
                " where id = 1 and version = " + version
        );
    }

    private void updateWithLock(Connection connection) throws SQLException {
        Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        ResultSet resultSet = statement.executeQuery("select * from optimistic_pessimistic_lock where id = 1 for update");
        resultSet.next();
        long version = resultSet.getLong("version");
        long newVersion = version + 1;
        String newName = String.valueOf(newVersion);
        resultSet.updateLong("version", newVersion);
        resultSet.updateString("name", newName);
        resultSet.updateRow();
        connection.commit();
    }

    private void withTable(SQLRunnable runnable) throws SQLException {
        try {
            withNewConnection(connection -> {
                Statement statement = connection.createStatement();
                statement.execute("create table optimistic_pessimistic_lock (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(3), version BIGINT)");
            });
            runnable.run();
        } finally {
            withNewConnection(connection -> {
                Statement statement = connection.createStatement();
                statement.execute("drop table optimistic_pessimistic_lock");
            });
        }
    }

    private void withNewConnection(SQLConsumer handler) throws SQLException {
        withNewConnection(handler, TRANSACTION_READ_COMMITTED);
    }

    private void withNewConnection(SQLConsumer handler, int isolationLevel) throws SQLException {
        withNewConnection(handler, isolationLevel, true);
    }

    private void withNewConnection(SQLConsumer handler, int isolationLevel, boolean autoCommit) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3307/gradesjava", "fomin_e", "123123123")) {
            connection.setAutoCommit(autoCommit);
            connection.setTransactionIsolation(isolationLevel);
            handler.accept(connection);
        }
    }

    interface SQLConsumer {
        void accept(Connection connection) throws SQLException;
    }

    interface SQLRunnable {
        void run() throws SQLException;
    }
}

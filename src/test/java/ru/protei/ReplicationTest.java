package ru.protei;

import com.mysql.cj.jdbc.Driver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;

public class ReplicationTest {
    @BeforeAll
    public static void beforeAll() throws SQLException {
        DriverManager.registerDriver(new Driver());
    }

    @Test
    public void test() throws SQLException {
        withTable(() -> {
            withNewConnection(connection -> {
                Statement statement = connection.createStatement();
                statement.executeUpdate("insert into user (id, name) values (1, '1')");
            });

            withNewConnection(connection -> {
                connection.setReadOnly(true);
                Statement statement = connection.createStatement();
                statement.executeQuery("select * from user");
            });
        });
    }

    private void withTable(SQLRunnable runnable) throws SQLException {
        withNewConnection(connection -> {
            Statement statement = connection.createStatement();
            statement.execute("drop table if exists user");
        });
        withNewConnection(connection -> {
            Statement statement = connection.createStatement();
            statement.execute("create table user (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(3))");
        });
        runnable.run();
    }

    private void withNewConnection(SQLConsumer handler) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql:replication://localhost:13306,localhost:13307/replica-test",
                "root",
                "123123123"
        )) {
            connection.setAutoCommit(true);
            connection.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
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

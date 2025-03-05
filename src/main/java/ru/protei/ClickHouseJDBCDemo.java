package ru.protei;

import java.sql.*;

public class ClickHouseJDBCDemo {
    private static final String DB_URL = "jdbc:clickhouse://localhost:18123/default";

    private final Connection conn;

    public ClickHouseJDBCDemo() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, "default", "changeme");
    }

    public Integer getAvgSalary() throws SQLException {
        String query = "SELECT avg(salary) FROM user where id < ?";
        try (PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setInt(1, 10);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}

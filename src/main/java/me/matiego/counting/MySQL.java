package me.matiego.counting;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.matiego.counting.utils.Logs;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQL {
    private final HikariDataSource ds;

    public MySQL(@NotNull String url, @NotNull String user, @NotNull String password) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setPoolName("Counting-Connection-Pool");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2058");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("allowLoadLocalInfile", "true");

        ds = new HikariDataSource(config);
        getConnection(); //test connection
    }

    public void close() {
        ds.close();
    }

    public @NotNull Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public boolean createTables() {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS counting_channels(chn VARCHAR(20) NOT NULL, guild VARCHAR(20) NOT NULL, type VARCHAR(30) NOT NULL, url VARCHAR(200) NOT NULL, PRIMARY KEY (chn))")) {
                stmt.execute();
            }
            try (PreparedStatement stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS counting_user_ranking(id VARCHAR(20) NOT NULL, guild VARCHAR(20) NOT NULL, score INT NOT NULL, CONSTRAINT counting_user_ranking_const UNIQUE (id, guild))")) {
                stmt.execute();
            }
            for (Dictionary.Type value : Dictionary.Type.values()) {
                try (PreparedStatement stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS counting_" + value + "_list(word VARCHAR(1000) NOT NULL, guild VARCHAR(20), CONSTRAINT counting_" + value + "const UNIQUE (word, guild))")) {
                    stmt.execute();
                }
                if (value.isDictionarySupported()) {
                    try (PreparedStatement stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS counting_" + value + "_dict(word VARCHAR(1000) NOT NULL, PRIMARY KEY (word))")) {
                        stmt.execute();
                    }
                }
            }
        } catch (SQLException e) {
            Logs.error("Failed to create database tables.", e);
            return false;
        }
        return true;
    }
}

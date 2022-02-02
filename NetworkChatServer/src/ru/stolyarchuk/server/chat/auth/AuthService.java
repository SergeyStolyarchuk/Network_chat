package ru.stolyarchuk.server.chat.auth;

import java.sql.*;


public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public AuthService() {
        try {
            connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:auth_user_BD.db");
        stmt = connection.createStatement();
    }


    public void disconnect() {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getUserNameByLoginAndPassword(String login, String password) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM users WHERE login = ? and pass = ?");
            ps.setString(1, login);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            return rs.getString(4);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


}

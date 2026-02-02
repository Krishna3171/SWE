package com.msa.main;

import com.msa.db.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {

        try {
            Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(
                    "SELECT message FROM test_connection");

            while (rs.next()) {
                System.out.println(rs.getString("message"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

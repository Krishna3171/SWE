import java.sql.*;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String user = "postgres.oacbzjwqzwhsndkoztoo";
        String pass = "Telsartan@2905";
        
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            ResultSet rs = c.createStatement().executeQuery("SELECT username, password, role FROM App_User;");
            while (rs.next()) {
                System.out.println(rs.getString("username") + " : " + rs.getString("password") + " : " + rs.getString("role"));
            }
        }
    }
}


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
        System.getenv("DB_URL") != null
        ? System.getenv("DB_URL")
        : "jdbc:mysql://localhost:3306/banking_management_system";

    private static final String USER =
        System.getenv("DB_USER");

    private static final String PASSWORD =
        System.getenv("DB_PASSWORD");

    public static Connection getConnection() {
        try {
            if (USER == null || PASSWORD == null) {
                throw new SQLException(
                    "Database credentials are not configured."
                );
            }

            Connection con = DriverManager.getConnection(
                URL, USER, PASSWORD
            );

            System.out.println("MySQL Connected Successfully!");
            return con;

        } catch (SQLException e) {
            System.out.println("Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }
}

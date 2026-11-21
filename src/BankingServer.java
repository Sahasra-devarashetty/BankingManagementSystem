import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class BankingServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0
        );

        server.createContext("/api/create-account",
            BankingServer::createAccount);

         server.createContext("/api/deposit",
            BankingServer::depositMoney);

            server.createContext("/api/withdraw",
             BankingServer::withdrawMoney);
             server.createContext("/api/balance",
              BankingServer::checkBalance);
              server.createContext("/api/transactions",
              BankingServer::transactionHistory);
              server.createContext("/api/dashboard",
              BankingServer::dashboardStats);
              server.createContext("/api/transfer",
             BankingServer::transferMoney);
        server.createContext("/", BankingServer::serveWebsite);

        server.start();

        System.out.println("Banking Server running at http://localhost:8080");
    }

    private static void createAccount(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            Map<String, String> data = new HashMap<>();

            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    data.put(
                        URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    );
                }
            }

            String name = data.get("customerName");
            String email = data.get("email");
            String phone = data.get("phone");
            String dob = data.get("dateOfBirth");
            String accountType = data.get("accountType");
            int branchId = Integer.parseInt(data.get("branchId"));

            try (Connection con = DBConnection.getConnection()) {

                if (con == null) {
                    throw new SQLException("Database connection failed");
                }

                con.setAutoCommit(false);

                try {
                    String customerSql =
                        "INSERT INTO customers " +
                        "(full_name, phone, email, date_of_birth) " +
                        "VALUES (?, ?, ?, ?)";

                    int customerId;

                    try (PreparedStatement ps =
                            con.prepareStatement(
                                customerSql,
                                Statement.RETURN_GENERATED_KEYS
                            )) {

                        ps.setString(1, name);
                        ps.setString(2, phone);
                        ps.setString(3, email);
                        ps.setDate(4,java.sql.Date.valueOf(dob));

                        ps.executeUpdate();

                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException(
                                    "Customer ID was not generated"
                                );
                            }
                            customerId = keys.getInt(1);
                        }
                    }

                    String accountNumber =
                        "ACC" + System.currentTimeMillis();

                    String accountSql =
                        "INSERT INTO accounts " +
                        "(account_number, customer_id, branch_id, " +
                        "account_type) VALUES (?, ?, ?, ?)";

                    try (PreparedStatement ps =
                            con.prepareStatement(accountSql)) {

                        ps.setString(1, accountNumber);
                        ps.setInt(2, customerId);
                        ps.setInt(3, branchId);
                        ps.setString(4, accountType);

                        ps.executeUpdate();
                    }

                    con.commit();
                    sendResponse(exchange, 200,
                        "Account created successfully! Account Number: "
                        + accountNumber);

                } catch (Exception e) {
                    con.rollback();
                    throw e;
                } finally {
                    con.setAutoCommit(true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500,
                "Account creation failed: " + e.getMessage());
        }
    }
 private static void depositMoney(HttpExchange exchange)
        throws IOException {

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendResponse(exchange, 405, "Method not allowed");
        return;
    }

    try {
        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );
 Map<String, String> data = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                data.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                );
            }
        }

        String accountNumber = data.get("accountNumber");
        double amount = Double.parseDouble(data.get("amount"));
 if (accountNumber == null || accountNumber.isBlank()
                || !Double.isFinite(amount) || amount <= 0) {
            sendResponse(exchange, 400, "Enter a valid account number and amount.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed");
            }

            con.setAutoCommit(false);

            try {
                int accountId;
 String findAccount =
                    "SELECT account_id FROM accounts " +
                    "WHERE account_number = ? FOR UPDATE";

                try (PreparedStatement ps =
                        con.prepareStatement(findAccount)) {

                    ps.setString(1, accountNumber);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            sendResponse(exchange, 404, "Account not found.");
                            return;
                        }
           accountId = rs.getInt("account_id");
                    }
                }

                String updateBalance =
                    "UPDATE accounts SET balance = balance + ? " +
                    "WHERE account_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(updateBalance)) {

                    ps.setDouble(1, amount);
                    ps.setInt(2, accountId);
                    ps.executeUpdate();
                 }

                String insertTransaction =
                    "INSERT INTO transactions " +
                    "(account_id, transaction_type, amount, " +
                    "transaction_date, description) " +
                    "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)";

                try (PreparedStatement ps =
                        con.prepareStatement(insertTransaction)) {

                    ps.setInt(1, accountId);
                    ps.setString(2, "Deposit");
                    ps.setDouble(3, amount);
                    ps.setString(4, "Money deposited");
                    ps.executeUpdate();
   }

                con.commit();

                sendResponse(exchange, 200,
                    "Deposit successful! Amount: " + amount);

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
 } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500, "Deposit failed: " + e.getMessage());
    }
}
    
private static void withdrawMoney(HttpExchange exchange)
        throws IOException {

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendResponse(exchange, 405, "Method not allowed");
        return;
    }

    try {
        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> data = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                data.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                );
            }
        }

        String accountNumber = data.get("accountNumber");
        String amountText = data.get("amount");

        if (accountNumber == null || accountNumber.isBlank()
                || amountText == null || amountText.isBlank()) {
            sendResponse(exchange, 400, "Enter account number and amount.");
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, "Enter a valid amount.");
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            sendResponse(exchange, 400, "Enter a valid withdrawal amount.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed");
            }

            con.setAutoCommit(false);

            try {
                int accountId;
                double balance;

                String findAccount =
                    "SELECT account_id, balance FROM accounts " +
                    "WHERE account_number = ? FOR UPDATE";

                try (PreparedStatement ps =
                        con.prepareStatement(findAccount)) {

                    ps.setString(1, accountNumber);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            sendResponse(exchange, 404, "Account not found.");
                            return;
                        }

                        accountId = rs.getInt("account_id");
                        balance = rs.getDouble("balance");
                    }
                }

                if (balance < amount) {
                    con.rollback();
                    sendResponse(exchange, 400, "Insufficient balance.");
                    return;
                }

                String updateBalance =
                    "UPDATE accounts SET balance = balance - ? " +
                    "WHERE account_id = ? AND balance >= ?";

                try (PreparedStatement ps =
                        con.prepareStatement(updateBalance)) {

                    ps.setDouble(1, amount);
                    ps.setInt(2, accountId);
                    ps.setDouble(3, amount);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Withdrawal could not be completed.");
                    }
                }

                String insertTransaction =
                    "INSERT INTO transactions " +
                    "(account_id, transaction_type, amount, " +
                    "transaction_date, description) " +
                    "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)";

                try (PreparedStatement ps =
                        con.prepareStatement(insertTransaction)) {

                    ps.setInt(1, accountId);
                    ps.setString(2, "Withdrawal");
                    ps.setDouble(3, amount);
                    ps.setString(4, "Money withdrawn");
                    ps.executeUpdate();
                }

                con.commit();

                sendResponse(exchange, 200,
                    "Withdrawal successful! Amount: " + amount);

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500,
            "Withdrawal failed: " + e.getMessage());
    }
}

private static void checkBalance(HttpExchange exchange)
        throws IOException {

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendResponse(exchange, 405, "Method not allowed");
        return;
    }

    try {
        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> data = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                data.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                );
            }
        }

        String accountNumber = data.get("accountNumber");

        if (accountNumber == null || accountNumber.isBlank()) {
            sendResponse(exchange, 400, "Enter account number.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            String sql =
                "SELECT balance FROM accounts WHERE account_number = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, accountNumber);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        String balance = rs.getString("balance");

                        sendResponse(exchange, 200,
                            "Current balance: Rs. " + balance);
                    } else {
                        sendResponse(exchange, 404, "Account not found.");
                    }
                }
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500,
            "Unable to check balance: " + e.getMessage());
    }
}
private static void transactionHistory(HttpExchange exchange)
        throws IOException {

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendResponse(exchange, 405, "Method not allowed");
        return;
    }

    try {
        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> data = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                data.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                );
            }
        }

        String accountNumber = data.get("accountNumber");

        if (accountNumber == null || accountNumber.isBlank()) {
            sendResponse(exchange, 400, "Enter account number.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            String sql =
                "SELECT t.transaction_id, t.transaction_type, " +
                "t.amount, t.transaction_date, t.description " +
                "FROM accounts a " +
                "JOIN transactions t ON a.account_id = t.account_id " +
                "WHERE a.account_number = ? " +
                "ORDER BY t.transaction_date DESC";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, accountNumber);

                try (ResultSet rs = ps.executeQuery()) {

                    StringBuilder result = new StringBuilder();

                    while (rs.next()) {
                        result.append("Transaction ID: ")
                            .append(rs.getInt("transaction_id"))
                            .append(" | Type: ")
                            .append(rs.getString("transaction_type"))
                            .append(" | Amount: Rs. ")
                            .append(rs.getBigDecimal("amount"))
                            .append(" | Date: ")
                            .append(rs.getTimestamp("transaction_date"))
                            .append(" | Description: ")
                            .append(rs.getString("description"))
                            .append("\n");
                    }

                    if (result.length() == 0) {
                        sendResponse(exchange, 200,
                            "No transactions found for this account.");
                    } else {
                        sendResponse(exchange, 200, result.toString());
                    }
                }
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500,
            "Unable to retrieve transaction history: " + e.getMessage());
    }
}
private static void transferMoney(HttpExchange exchange)
        throws IOException {

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendResponse(exchange, 405, "Method not allowed");
        return;
    }

    try {
        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> data = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                data.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                );
            }
        }

        String senderAccount = data.get("senderAccount");
        String receiverAccount = data.get("receiverAccount");
        java.math.BigDecimal amount =
            new java.math.BigDecimal(data.get("amount"));

        if (senderAccount == null || receiverAccount == null ||
                senderAccount.isBlank() || receiverAccount.isBlank()) {
            sendResponse(exchange, 400, "Enter both account numbers.");
            return;
        }

        if (senderAccount.equals(receiverAccount)) {
            sendResponse(exchange, 400,
                "Sender and receiver accounts must be different.");
            return;
        }

        if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            sendResponse(exchange, 400,
                "Transfer amount must be greater than zero.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {
                Map<String, Integer> accountIds = new HashMap<>();
                Map<String, java.math.BigDecimal> balances = new HashMap<>();

                String selectSql =
                    "SELECT account_id, account_number, balance " +
                    "FROM accounts " +
                    "WHERE account_number IN (?, ?) " +
                    "ORDER BY account_id FOR UPDATE";

                try (PreparedStatement ps =
                        con.prepareStatement(selectSql)) {

                    ps.setString(1, senderAccount);
                    ps.setString(2, receiverAccount);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String number = rs.getString("account_number");

                            accountIds.put(number,
                                rs.getInt("account_id"));

                            balances.put(number,
                                rs.getBigDecimal("balance"));
                        }
                    }
                }

                if (!accountIds.containsKey(senderAccount) ||
                        !accountIds.containsKey(receiverAccount)) {
                    con.rollback();
                    sendResponse(exchange, 404,
                        "Sender or receiver account not found.");
                    return;
                }

                if (balances.get(senderAccount).compareTo(amount) < 0) {
                    con.rollback();
                    sendResponse(exchange, 400,
                        "Insufficient balance.");
                    return;
                }

                String updateSql =
                    "UPDATE accounts SET balance = balance - ? " +
                    "WHERE account_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(updateSql)) {
                    ps.setBigDecimal(1, amount);
                    ps.setInt(2, accountIds.get(senderAccount));
                    ps.executeUpdate();
                }

                String creditSql =
                    "UPDATE accounts SET balance = balance + ? " +
                    "WHERE account_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(creditSql)) {
                    ps.setBigDecimal(1, amount);
                    ps.setInt(2, accountIds.get(receiverAccount));
                    ps.executeUpdate();
                }

                String insertSql =
                    "INSERT INTO transactions " +
                    "(account_id, transaction_type, amount, " +
                    "transaction_date, description) " +
                    "VALUES (?, ?, ?, NOW(), ?)";

                try (PreparedStatement ps =
                        con.prepareStatement(insertSql)) {

                    ps.setInt(1, accountIds.get(senderAccount));
                    ps.setString(2, "Transfer");
                    ps.setBigDecimal(3, amount);
                    ps.setString(4, "Money transferred to " + receiverAccount);
                    ps.executeUpdate();

                    ps.setInt(1, accountIds.get(receiverAccount));
                    ps.setString(2, "Transfer");
                    ps.setBigDecimal(3, amount);
                    ps.setString(4, "Money received from " + senderAccount);
                    ps.executeUpdate();
                }

                con.commit();

                sendResponse(exchange, 200,
                    "Transfer successful! Amount: Rs. " + amount);

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500,
            "Transfer failed: " + e.getMessage());
    }
}
private static void dashboardStats(HttpExchange exchange)
        throws IOException {

    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
        sendResponse(exchange, 405, "Method not allowed");
        return;
    }

    try (Connection con = DBConnection.getConnection()) {

        String sql =
            "SELECT " +
            "(SELECT COUNT(*) FROM accounts) AS total_accounts, " +
            "(SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE transaction_type = 'deposit') AS total_deposits, " +
            "(SELECT COUNT(*) FROM transactions) AS total_transactions";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String result =
                    rs.getInt("total_accounts") + "," +
                    rs.getBigDecimal("total_deposits") + "," +
                    rs.getInt("total_transactions");

                sendResponse(exchange, 200, result);
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500,
            "Unable to load dashboard statistics: " + e.getMessage());
    }
}
    private static void serveWebsite(HttpExchange exchange)
            throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        Path file = Paths.get("web").resolve(path.substring(1)).normalize();

        if (!file.startsWith(Paths.get("web").toAbsolutePath().normalize())
                && !file.startsWith(Paths.get("web").normalize())) {
            sendResponse(exchange, 403, "Forbidden");
            return;
        }

        if (!Files.exists(file) || Files.isDirectory(file)) {
            sendResponse(exchange, 404, "File not found");
            return;
        }

        String type = "text/plain";

        if (path.endsWith(".html")) type = "text/html";
        else if (path.endsWith(".css")) type = "text/css";
        else if (path.endsWith(".js")) type = "application/javascript";

        exchange.getResponseHeaders().set(
            "Content-Type", type + "; charset=UTF-8"
        );

        byte[] content = Files.readAllBytes(file);
        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(content);
        }
    }

    private static void sendResponse(
            HttpExchange exchange, int status, String message)
            throws IOException {

        byte[] response = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "text/plain; charset=UTF-8"
        );
        exchange.sendResponseHeaders(status,response.length);
        try(OutputStream out =
            exchange.getResponseBody()){
                out.write(response);
            }
        
            
        }
    }



    
import java.sql.*;
import java.util.Scanner;

public class ATM {
    private static Connection conn;
    private static int currentUserId;
    private static double currentBalance;

    public static void main(String[] args) {
        try {
            // Connect to the database
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/atm_system", "root", "root");

            if (login()) {
                boolean running = true;
                while (running) {
                    showMenu();
                    Scanner sc = new Scanner(System.in);
                    int choice = sc.nextInt();
                    switch (choice) {
                        case 1 -> showTransactionHistory();
                        case 2 -> withdraw();
                        case 3 -> deposit();
                        case 4 -> transfer();
                        case 5 -> {
                            running = false;
                            System.out.println("You have exited the system.");
                            recordSession("Logout");
                        }
                        default -> System.out.println("Invalid choice. Try again.");
                    }
                }
            } else {
                System.out.println("Goodbye!");
            }
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        }
    }

    // Login method with session logging
    private static boolean login() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter User ID:");
        String userId = sc.nextLine();
        System.out.println("Enter PIN:");
        String pin = sc.nextLine();

        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT id, balance FROM users WHERE id = ? AND pin = ?");
            stmt.setString(1, userId);
            stmt.setString(2, pin);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                currentUserId = rs.getInt("id");
                currentBalance = rs.getDouble("balance");
                System.out.println("Login successful! Your current balance is: " + currentBalance);
                recordSession("Login");
                return true;
            } else {
                System.out.println("Invalid credentials.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error during login: " + e.getMessage());
            return false;
        }
    }

    // Record user sessions (login/logout)
    private static void recordSession(String action) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO user_sessions (user_id, session_action, session_time) VALUES (?, ?, NOW())");
            stmt.setInt(1, currentUserId);
            stmt.setString(2, action);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error recording session: " + e.getMessage());
        }
    }

    // Show the ATM menu
    private static void showMenu() {
        System.out.println("\nATM Menu:");
        System.out.println("1. Transaction History");
        System.out.println("2. Withdraw");
        System.out.println("3. Deposit");
        System.out.println("4. Transfer");
        System.out.println("5. Quit");
        System.out.print("Enter your choice: ");
    }

    // Display transaction history
    private static void showTransactionHistory() {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM transactions WHERE user_id = ?");
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();

            System.out.println("Transaction History:");
            while (rs.next()) {
                System.out.printf("%s - %s: %.2f\n", rs.getTimestamp("transaction_date"), rs.getString("transaction_type"), rs.getDouble("amount"));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching transaction history: " + e.getMessage());
        }
    }

    // Withdraw money from the account
    private static void withdraw() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter amount to withdraw: ");
        double amount = sc.nextDouble();

        if (updateBalance(-amount)) {
            recordTransaction("Withdraw", amount);
            System.out.println("Withdrawal successful. Your new balance is: " + currentBalance);
        }
    }

    // Deposit money into the account
    private static void deposit() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter amount to deposit: ");
        double amount = sc.nextDouble();

        if (updateBalance(amount)) {
            recordTransaction("Deposit", amount);
            System.out.println("Deposit successful. Your new balance is: " + currentBalance);
        }
    }

    // Transfer money to another user
    private static void transfer() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter recipient user ID: ");
        int recipientId = sc.nextInt();
        System.out.println("Enter amount to transfer: ");
        double amount = sc.nextDouble();

        if (updateBalance(-amount)) {
            try {
                PreparedStatement stmt = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE id = ?");
                stmt.setDouble(1, amount);
                stmt.setInt(2, recipientId);
                stmt.executeUpdate();

                recordTransaction("Transfer", amount);
                System.out.println("Transfer successful. Your new balance is: " + currentBalance);
            } catch (SQLException e) {
                System.out.println("Error during transfer: " + e.getMessage());
            }
        }
    }

    // Update the user's balance after a transaction
    private static boolean updateBalance(double amount) {
        try {
            if (currentBalance + amount >= 0) {
                PreparedStatement stmt = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE id = ?");
                stmt.setDouble(1, amount);
                stmt.setInt(2, currentUserId);
                stmt.executeUpdate();
                currentBalance += amount;
                return true;
            } else {
                System.out.println("Insufficient balance.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error updating balance: " + e.getMessage());
            return false;
        }
    }

    // Record a transaction in the transaction history
    private static void recordTransaction(String type, double amount) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO transactions (user_id, transaction_type, amount) VALUES (?, ?, ?)");
            stmt.setInt(1, currentUserId);
            stmt.setString(2, type);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error recording transaction: " + e.getMessage());
        }
    }
}

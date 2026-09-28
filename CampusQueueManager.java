import java.sql.*;
import java.util.Scanner;

public class CampusQueueManager {

    static final String URL = "jdbc:mysql://localhost:3306/campus_queue";
    static final String USER = "queue_user";
    static final String PASSWORD = "YOUR PASSWORD";

    static Scanner sc = new Scanner(System.in);

    // Connect to database
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Show all services
    public static void showServices() {

        String sql = "SELECT * FROM services";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n===== AVAILABLE SERVICES =====");

            while (rs.next()) {

                System.out.println(
                        rs.getInt("service_id") + ". " +
                                rs.getString("service_name") +
                                " | Location: " +
                                rs.getString("location") +
                                " | Avg Time: " +
                                rs.getInt("avg_service_time") +
                                " min"
                );
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Register a new student
    public static void registerStudent() {

        System.out.print("Enter student name: ");
        String name = sc.nextLine();

        System.out.print("Enter email: ");
        String email = sc.nextLine();

        String sql =
                "INSERT INTO users (name, email, role) VALUES (?, ?, 'STUDENT')";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) {
                System.out.println(
                        "Student registered successfully!"
                );
                System.out.println(
                        "Your User ID: " + rs.getInt(1)
                );
            }

        } catch (SQLException e) {

            if (e.getMessage().contains("Duplicate")) {
                System.out.println("Email already exists.");
            } else {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Get today's queue for a service
    public static int getQueue(int serviceId) {

        String checkSql =
                "SELECT queue_id FROM queues " +
                        "WHERE service_id = ? AND queue_date = CURDATE()";

        String createSql =
                "INSERT INTO queues (service_id, queue_date) " +
                        "VALUES (?, CURDATE())";

        try (Connection con = getConnection()) {

            // Check whether today's queue already exists
            try (PreparedStatement ps =
                         con.prepareStatement(checkSql)) {

                ps.setInt(1, serviceId);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return rs.getInt("queue_id");
                }
            }

            // Create today's queue
            try (PreparedStatement ps =
                         con.prepareStatement(
                                 createSql,
                                 Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, serviceId);

                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return -1;
    }

    // Join a queue
    public static void joinQueue(int userId) {

        showServices();

        System.out.print("\nEnter Service ID: ");
        int serviceId = sc.nextInt();

        int queueId = getQueue(serviceId);

        if (queueId == -1) {
            System.out.println("Could not create/find queue.");
            return;
        }

        String tokenSql =
                "SELECT COALESCE(MAX(token_number), 0) + 1 " +
                        "FROM tickets WHERE queue_id = ?";

        String insertSql =
                "INSERT INTO tickets " +
                        "(queue_id, user_id, token_number, status) " +
                        "VALUES (?, ?, ?, 'WAITING')";

        try (Connection con = getConnection()) {

            int tokenNumber = 1;

            // Generate next token
            try (PreparedStatement ps =
                         con.prepareStatement(tokenSql)) {

                ps.setInt(1, queueId);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    tokenNumber = rs.getInt(1);
                }
            }

            // Create ticket
            try (PreparedStatement ps =
                         con.prepareStatement(
                                 insertSql,
                                 Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, queueId);
                ps.setInt(2, userId);
                ps.setInt(3, tokenNumber);

                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()) {

                    int ticketId = rs.getInt(1);

                    addHistory(
                            con,
                            ticketId,
                            "JOINED"
                    );

                    System.out.println(
                            "\n===== QUEUE JOINED ====="
                    );

                    System.out.println(
                            "Ticket ID: " + ticketId
                    );

                    System.out.println(
                            "Your Token: " + tokenNumber
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // View waiting queue
    public static void viewQueue() {

        showServices();

        System.out.print("\nEnter Service ID: ");
        int serviceId = sc.nextInt();

        String sql =
                "SELECT t.ticket_id, t.token_number, " +
                        "u.name, t.status, t.joined_at " +
                        "FROM tickets t " +
                        "JOIN users u ON t.user_id = u.user_id " +
                        "JOIN queues q ON t.queue_id = q.queue_id " +
                        "WHERE q.service_id = ? " +
                        "AND q.queue_date = CURDATE() " +
                        "AND t.status IN ('WAITING', 'CALLED') " +
                        "ORDER BY t.token_number";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, serviceId);

            ResultSet rs = ps.executeQuery();

            System.out.println("\n===== CURRENT QUEUE =====");

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.println(
                        "Ticket: " +
                                rs.getInt("ticket_id") +
                                " | Token: " +
                                rs.getInt("token_number") +
                                " | Name: " +
                                rs.getString("name") +
                                " | Status: " +
                                rs.getString("status")
                );
            }

            if (!found) {
                System.out.println("No students in queue.");
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Call next student
    public static void callNext() {

        showServices();

        System.out.print("\nEnter Service ID: ");
        int serviceId = sc.nextInt();

        String findSql =
                "SELECT t.ticket_id, t.token_number, u.name " +
                        "FROM tickets t " +
                        "JOIN users u ON t.user_id = u.user_id " +
                        "JOIN queues q ON t.queue_id = q.queue_id " +
                        "WHERE q.service_id = ? " +
                        "AND q.queue_date = CURDATE() " +
                        "AND t.status = 'WAITING' " +
                        "ORDER BY t.token_number " +
                        "LIMIT 1";

        String updateSql =
                "UPDATE tickets " +
                        "SET status = 'CALLED', called_at = NOW() " +
                        "WHERE ticket_id = ?";

        try (Connection con = getConnection()) {

            int ticketId = -1;

            try (PreparedStatement ps =
                         con.prepareStatement(findSql)) {

                ps.setInt(1, serviceId);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {

                    ticketId = rs.getInt("ticket_id");

                    System.out.println(
                            "\nCalling Token " +
                                    rs.getInt("token_number") +
                                    " - " +
                                    rs.getString("name")
                    );
                }
            }

            if (ticketId == -1) {
                System.out.println("No waiting students.");
                return;
            }

            try (PreparedStatement ps =
                         con.prepareStatement(updateSql)) {

                ps.setInt(1, ticketId);
                ps.executeUpdate();

                addHistory(
                        con,
                        ticketId,
                        "CALLED"
                );
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Complete a ticket
    public static void completeTicket() {

        System.out.print("Enter Ticket ID: ");
        int ticketId = sc.nextInt();

        String sql =
                "UPDATE tickets " +
                        "SET status = 'COMPLETED', completed_at = NOW() " +
                        "WHERE ticket_id = ? " +
                        "AND status = 'CALLED'";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            int rows = ps.executeUpdate();

            if (rows > 0) {

                addHistory(
                        con,
                        ticketId,
                        "COMPLETED"
                );

                System.out.println(
                        "Ticket completed successfully."
                );

            } else {

                System.out.println(
                        "Ticket not found or not currently being served."
                );
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Cancel a ticket
    public static void cancelTicket() {

        System.out.print("Enter Ticket ID: ");
        int ticketId = sc.nextInt();

        String sql =
                "UPDATE tickets " +
                        "SET status = 'CANCELLED' " +
                        "WHERE ticket_id = ? " +
                        "AND status = 'WAITING'";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ticketId);

            int rows = ps.executeUpdate();

            if (rows > 0) {

                addHistory(
                        con,
                        ticketId,
                        "CANCELLED"
                );

                System.out.println(
                        "Ticket cancelled successfully."
                );

            } else {

                System.out.println(
                        "Ticket not found or already processed."
                );
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Add ticket history
    public static void addHistory(
            Connection con,
            int ticketId,
            String action
    ) {

        String sql =
                "INSERT INTO queue_history " +
                        "(ticket_id, action) VALUES (?, ?)";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setInt(1, ticketId);
            ps.setString(2, action);

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                    "History Error: " + e.getMessage()
            );
        }
    }

    // Main menu
    public static void main(String[] args) {

        while (true) {

            System.out.println(
                    "\n================================"
            );
            System.out.println(
                    "       CAMPUS QUEUE MANAGER"
            );
            System.out.println(
                    "================================"
            );

            System.out.println("1. Register Student");
            System.out.println("2. View Services");
            System.out.println("3. Join Queue");
            System.out.println("4. View Queue");
            System.out.println("5. Call Next Student");
            System.out.println("6. Complete Ticket");
            System.out.println("7. Cancel Ticket");
            System.out.println("8. Exit");

            System.out.print("\nEnter choice: ");

            int choice = sc.nextInt();

            switch (choice) {

                case 1:
                    sc.nextLine();
                    registerStudent();
                    break;

                case 2:
                    showServices();
                    break;

                case 3:
                    System.out.print(
                            "Enter your User ID: "
                    );
                    int userId = sc.nextInt();
                    joinQueue(userId);
                    break;

                case 4:
                    viewQueue();
                    break;

                case 5:
                    callNext();
                    break;

                case 6:
                    completeTicket();
                    break;

                case 7:
                    cancelTicket();
                    break;

                case 8:
                    System.out.println(
                            "Thank you for using Campus Queue Manager!"
                    );
                    sc.close();
                    return;

                default:
                    System.out.println(
                            "Invalid choice."
                    );
            }
        }
    }
}

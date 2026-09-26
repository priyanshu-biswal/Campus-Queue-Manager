# Campus Queue Manager

A **Java + JDBC + MySQL** based digital queue management system designed for college offices.

Instead of students physically standing in long queues at places such as the **Examination Office, Accounts Office, Administration, or Library**, the system gives each student a digital token and allows staff to manage the queue.

---

# 1. How the Project Works

The basic workflow is:

```text
Student
   ↓
Select College Service
   ↓
Join Queue
   ↓
System Generates Token
   ↓
Student Waits
   ↓
Staff Calls Next Student
   ↓
Student Gets Served
   ↓
Staff Completes Ticket
```

### Example

Suppose Priyanshu wants to visit the Library.

```text
Priyanshu
    ↓
Selects Library
    ↓
Joins Queue
    ↓
Gets Token: 1
    ↓
Waits
    ↓
Library Staff calls Token 1
    ↓
Service completed
```

If Ashaaz joins after Priyanshu:

```text
Priyanshu → Library → Token 1
Ashaaz    → Library → Token 2
Atif      → Library → Token 3
```

The staff member processes them in queue order.

---

# 2. Services Available

The system currently has four college services:

| Service            | Location | Average Service Time |
| ------------------ | -------- | -------------------: |
| Examination Office | Block A  |                5 min |
| Accounts Office    | Block B  |                7 min |
| Administration     | Block A  |                5 min |
| Library            | Block C  |                3 min |

These services are stored in the `services` table.

---

# 3. Database

The project uses **MySQL**.

Database name:

```text
campus_queue
```

The database contains the following main tables:

```text
users
services
queues
tickets
queue_history
```

---

# 4. Database Tables

## users

Stores all users of the system.

```text
users
│
├── user_id
├── name
├── email
└── role
```

Example:

```text
101 | Priyanshu | priyanshu@college.com | STUDENT
102 | Ashaaz    | ashaaz@college.com    | STUDENT
103 | Atif      | atif@college.com      | STUDENT
104 | Muzammil  | muzammil@college.com  | STUDENT
105 | Admin     | admin@college.com     | ADMIN
106 | Library Staff | library@college.com | STAFF
```

The `role` tells the application whether the user is a student, staff member, or admin.

---

## services

Stores the college services.

```text
services
│
├── service_id
├── service_name
├── location
└── avg_service_time
```

For example:

```text
1 | Examination Office | Block A | 5
2 | Accounts Office    | Block B | 7
3 | Administration     | Block A | 5
4 | Library             | Block C | 3
```

---

## queues

Represents the queue for a particular service on a particular day.

```text
queues
│
├── queue_id
├── service_id
├── queue_date
├── current_token
└── status
```

For example:

```text
Queue ID: 1
Service: Library
Date: 2026-09-26
Status: OPEN
```

The `service_id` connects the queue to the `services` table.

---

## tickets

This is the most important table for the actual queue system.

Every time a student joins a queue, a **ticket** is created.

```text
tickets
│
├── ticket_id
├── queue_id
├── user_id
├── token_number
├── joined_at
├── called_at
├── completed_at
└── status
```

Example:

```text
Ticket 1
Student: Priyanshu
Service: Library
Token: 1
Status: WAITING
```

When the staff calls the student:

```text
Status: WAITING → CALLED
```

After service:

```text
Status: CALLED → COMPLETED
```

---

## queue_history

Stores important actions performed on tickets.

```text
queue_history
│
├── history_id
├── ticket_id
├── action
└── action_time
```

For example:

```text
Ticket 1 → JOINED
Ticket 1 → CALLED
Ticket 1 → COMPLETED
```

This allows the system to maintain a history of what happened to each ticket.

---

# 5. Relationship Between Tables

The basic database relationship is:

```text
USERS
  │
  │ user_id
  ↓
TICKETS
  │
  │ queue_id
  ↓
QUEUES
  │
  │ service_id
  ↓
SERVICES
```

And:

```text
TICKETS
   │
   │ ticket_id
   ↓
QUEUE_HISTORY
```

In simple terms:

```text
Student
   ↓
Ticket
   ↓
Queue
   ↓
Service
```

This is how the database connects a student to the service they are waiting for.

---

# 6. Database Setup

## Step 1 — Create Database

Open MySQL and run:

```sql
CREATE DATABASE campus_queue;
```

Then:

```sql
USE campus_queue;
```

---

## Step 2 — Create Database User

Create the user:

```sql
CREATE USER 'queue_user'@'localhost'
IDENTIFIED BY 'YOUR_PASSWORD';
```

Give the user access:

```sql
GRANT ALL PRIVILEGES ON campus_queue.* 
TO 'queue_user'@'localhost';

FLUSH PRIVILEGES;
```

---

## Step 3 — Import the SQL File

The repository contains:

```text
database/campus_queue.sql
```

This file contains the database structure and sample data.

From the MySQL terminal:

```bash
mysql -u queue_user -p campus_queue < database/campus_queue.sql
```

Enter the password when MySQL asks for it.

Alternatively, inside MySQL:

```sql
USE campus_queue;

SOURCE database/campus_queue.sql;
```

After importing, check:

```sql
SHOW TABLES;
```

You should see:

```text
users
services
queues
tickets
queue_history
```

---

# 7. JDBC Connection

The Java application connects to MySQL using JDBC.

The connection looks like:

```java
Class.forName("com.mysql.cj.jdbc.Driver");

Connection con = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/campus_queue",
    "queue_user",
    "YOUR_PASSWORD"
);
```

The process is:

```text
Java Program
     ↓
JDBC Driver
     ↓
MySQL Server
     ↓
campus_queue Database
     ↓
Tables
```

The Java program sends SQL queries to MySQL and receives the results.

---

# 8. MySQL Connector/J

The project requires **MySQL Connector/J**.

In IntelliJ:

```text
Project
  ↓
lib
  ↓
mysql-connector-j-....jar
```

The JAR allows Java to communicate with MySQL.

Without the JDBC driver, the Java program cannot connect to the database.

---

# 9. Main Application Features

The application provides options such as:

```text
1. View All Users
2. Register Student
3. View Services
4. Join Queue
5. View Queue
6. Call Next Student
7. Complete Ticket
8. Cancel Ticket
9. Exit
```

### View All Users

Displays students, staff, and administrators stored in the `users` table.

### Register Student

Adds a new student to the `users` table.

### View Services

Displays available college services from the `services` table.

### Join Queue

The student enters their User ID and selects a service.

The system:

1. Finds today's queue.
2. Creates the queue if necessary.
3. Generates the next token.
4. Creates a ticket.
5. Stores the ticket in MySQL.

### View Queue

Displays students currently waiting for a service.

### Call Next Student

Staff selects this option.

The system finds the next waiting ticket and changes:

```text
WAITING → CALLED
```

It also records the calling time.

### Complete Ticket

After serving the student:

```text
CALLED → COMPLETED
```

The completion time is stored.

### Cancel Ticket

A waiting ticket can be cancelled:

```text
WAITING → CANCELLED
```

---

# 10. Example Presentation Demo

For the college presentation, use this simple scenario.

### Step 1

Show the database in MySQL.

Run:

```sql
USE campus_queue;

SELECT * FROM users;
SELECT * FROM services;
```

Explain that the database contains students and college services.

### Step 2

Run the Java application.

Select:

```text
3. View Services
```

Show:

```text
Examination Office
Accounts Office
Administration
Library
```

### Step 3

Join a queue.

Choose:

```text
4. Join Queue
```

Enter a student ID, for example:

```text
101
```

Select:

```text
Library
```

The system generates:

```text
Your Token Number: 1
```

### Step 4

Join another student.

For example:

```text
User ID: 102
Service: Library
Token: 2
```

### Step 5

View the queue.

Select:

```text
5. View Queue
```

Show:

```text
Token 1 → Priyanshu → WAITING
Token 2 → Ashaaz   → WAITING
```

### Step 6

Act as the staff member.

Select:

```text
6. Call Next Student
```

The system calls:

```text
Token 1 → Priyanshu
```

Status changes:

```text
WAITING → CALLED
```

### Step 7

Complete the service.

Select:

```text
7. Complete Ticket
```

The ticket becomes:

```text
COMPLETED
```

Now you can show MySQL:

```sql
SELECT * FROM tickets;
```

This demonstrates that the Java application actually changed the database.

---

# 11. What to Explain During Presentation

A simple explanation is:

> "Campus Queue Manager is a Java-based digital queue management system connected to MySQL using JDBC. Students can select a college service and join its queue. The system generates a unique token and stores the ticket in the database. Staff can then call the next student, complete the service, or cancel a ticket. The database maintains users, services, queues, tickets, and queue history."

Then explain the database:

> "The users table stores students and staff. The services table stores college services. The queues table represents the daily queue for each service. Whenever a student joins a queue, a ticket is created and connected to both the user and queue using foreign keys. The queue_history table records important actions performed on tickets."

---

# 12. Why JDBC Is Used

JDBC stands for **Java Database Connectivity**.

It acts as the connection between the Java application and MySQL.

```text
Java
  ↓
JDBC
  ↓
MySQL
```

For example, when a student joins a queue, Java executes an SQL `INSERT` statement to store the ticket in MySQL.

When the user selects "View Queue", Java executes a `SELECT` query and displays the result.

---

# 13. Technologies

* Java
* JDBC
* MySQL
* SQL
* IntelliJ IDEA
* MySQL Connector/J

---

# 14. Future Improvements

Possible future versions could include:

* Login system
* Separate student and staff interfaces
* Role-based access control
* Estimated waiting time
* Queue statistics
* Daily and monthly reports
* GUI application
* Web application
* Real-time queue updates
* Notifications when a student's turn is approaching

---

# 15. Author

**Priyanshu Biswal**

B.Tech Computer Science Engineering

Project: **Campus Queue Manager**

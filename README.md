# JavaChat

A Java Swing desktop chat application using TCP sockets, MySQL, JDBC, Maven, and an MVC-inspired structure.

## Requirements

- JDK 25
- MySQL 8.x (or a compatible MySQL server)
- IntelliJ IDEA
- Maven (IntelliJ can use its bundled Maven)

## 1. Create the database

Run `database/schema.sql` in MySQL Workbench.

## 2. Configure environment variables

Configure these variables for BOTH the `ChatServer` and `App` run configurations:

- `DB_URL=jdbc:mysql://localhost:3306/java_chat`
- `DB_USER=your_mysql_username`
- `DB_PASSWORD=your_mysql_password`

Optional:
- `CHAT_HOST=127.0.0.1`
- `CHAT_PORT=5050`

Do not commit your database password to Git.

## 3. Import the project

Open the `JavaChat` folder in IntelliJ IDEA and allow Maven to load dependencies.

## 4. Run the server first

Open `src/main/java/com/javachat/server/ChatServer.java` and run `main()`.

Expected console message:
`JavaChat server listening on port 5050`

## 5. Run the desktop client

Run `src/main/java/com/javachat/App.java`. Register a user. Run `App` again to create a second client, register another username, then log in and message the other user.

## Included features

- Registration and login with BCrypt password hashes
- TCP socket server with a separate handler per client
- JSON-lines protocol over TCP
- Private messages and persistent message history
- Online user list
- Swing login and chat windows
- MySQL persistence using JDBC and DAO classes

## Notes

- This is an educational project intended for local development and trusted networks.
- For use over the public internet, add TLS, rate limiting, stronger session management, deployment hardening, and operational monitoring.
- The first version supports text-only private chats, not group chats or file transfers.

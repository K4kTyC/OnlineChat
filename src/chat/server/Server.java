package chat.server;

import chat.server.MessageOperations.MessageRemover;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final File usersDBFilePath = new File("usersDB.txt");
    private static final Map<String, Integer> logpassDB = Collections.synchronizedMap(new HashMap<>());
    private static final List<String> moderators = Collections.synchronizedList(new ArrayList<>());
    private static final List<Session> onlineSessions = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> onlineClients = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Date> kickedClients = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, ChatRoom> chatRooms = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(23456)) {
            System.out.println("Server started!");
            try {
                loadUsersDB();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            chatRooms.put("guest", new ChatRoom("guest"));

            MessageRemover messageRemover = new MessageRemover(chatRooms);
            messageRemover.start();

            while (true) {
                try {
                    Session session = new Session(server.accept(), usersDBFilePath, logpassDB, moderators, onlineSessions, onlineClients, kickedClients, chatRooms);
                    session.start();
                    onlineSessions.add(session);
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
            messageRemover.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadUsersDB() throws FileNotFoundException {
        Scanner fileScanner = new Scanner(usersDBFilePath);
        while (fileScanner.hasNext()) {
            String[] userInfo = fileScanner.nextLine().split(" ");
            logpassDB.put(userInfo[0], Integer.parseInt(userInfo[1]));
            if ("moderator".equals(userInfo[2])) {
                moderators.add(userInfo[0]);
            }
        }
    }
}

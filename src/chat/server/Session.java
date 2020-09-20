package chat.server;

import chat.server.MessageOperations.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Session extends Thread {
    private final Socket socket;
    private final File usersDBFilepath;
    private final Map<String, Integer> logpassDB;
    private final List<String> moderators;
    private final List<Session> onlineSessions;
    private final List<String> onlineClients;
    private final Map<String, Date> kickedClients;
    private final Map<String, ChatRoom> chatRooms;

    private String clientLogin = "";
    private String role = "";
    private volatile String addresseeLogin = "";
    private ChatRoom chatRoom;
    private File chatFile;
    private volatile boolean isExit = false;

    private final CopyOnWriteArrayList<String> serverMsgList = new CopyOnWriteArrayList<>();
    private final List<String> historyMsgList = Collections.synchronizedList(new ArrayList<>());

    public Session(Socket socket, File usersDBFilepath, Map<String, Integer> logpassDB,
                   List<String> moderators, List<Session> onlineSessions,
                   List<String> onlineClients, Map<String, Date> kickedClients, Map<String, ChatRoom> chatRooms) {
        this.socket = socket;
        this.usersDBFilepath = usersDBFilepath;
        this.logpassDB = logpassDB;
        this.moderators = moderators;
        this.onlineSessions = onlineSessions;
        this.onlineClients = onlineClients;
        this.kickedClients = kickedClients;
        this.chatRooms = chatRooms;
        this.chatRoom = chatRooms.get("guest");
    }

    @Override
    public void run() {
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            if (!isExit) {
                MessageRetriever messageRetriever = new MessageRetriever(this, input);
                MessageSender messageSender = new MessageSender(this, output);
                MessageSaver messageSaver = new MessageSaver(this);
                messageRetriever.start();
                messageSender.start();
                messageSaver.start();
                messageRetriever.join();
                messageSender.join();
                messageSaver.join();

                disconnect();
                onlineClients.remove(clientLogin);
                onlineSessions.remove(this);
            }
            socket.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        if (getChatRoom().getType() == 1) {
            getChatRoom().getUsers().put(getClientLogin(), false);
        }
    }

    public File getUsersDBFilepath() {
        return usersDBFilepath;
    }

    public Map<String, Integer> getLogpassDB() {
        return logpassDB;
    }

    public List<String> getModerators() {
        return moderators;
    }

    public List<Session> getOnlineSessions() {
        return onlineSessions;
    }

    public List<String> getOnlineClients() {
        return onlineClients;
    }

    public Map<String, Date> getKickedClients() {
        return kickedClients;
    }

    public Map<String, ChatRoom> getChatRooms() {
        return chatRooms;
    }

    public String getClientLogin() {
        return clientLogin;
    }

    public void setClientLogin(String clientLogin) {
        this.clientLogin = clientLogin;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAddresseeLogin() {
        return addresseeLogin;
    }

    public void setAddresseeLogin(String addresseeLogin) {
        this.addresseeLogin = addresseeLogin;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public File getChatFile() {
        return chatFile;
    }

    public void setChatFile(File chatFile) {
        this.chatFile = chatFile;
    }

    public boolean isExit() {
        return isExit;
    }

    public void setExit(boolean exit) {
        isExit = exit;
    }

    public CopyOnWriteArrayList<String> getServerMsgList() {
        return serverMsgList;
    }

    public List<String> getHistoryMsgList() {
        return historyMsgList;
    }
}

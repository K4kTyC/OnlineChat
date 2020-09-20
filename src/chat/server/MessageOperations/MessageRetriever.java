package chat.server.MessageOperations;

import chat.server.ChatRoom;
import chat.server.Message;
import chat.server.Session;
import chat.server.SyncFileWriters.SyncDBFileWriter;
import chat.server.SyncFileWriters.SyncMsgFileWriter;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class MessageRetriever extends Thread {

    private final Session session;
    private final DataInputStream input;

    public MessageRetriever(Session session, DataInputStream input) {
        this.session = session;
        this.input = input;
    }

    @Override
    public void run() {
        session.getServerMsgList().add("Server: authorize or register");
        while (!session.isInterrupted() && !session.isExit()) {
            String msg;
            try {
                msg = input.readUTF().trim();
            } catch (IOException e) {
                e.printStackTrace();
                session.setExit(true);
                break;
            }
            if (session.getClientLogin().isEmpty()) {
                String[] commandArgs = msg.split("\\s+");
                if ("/registration".equals(commandArgs[0])) {
                    if (commandArgs.length != 3) {
                        session.getServerMsgList().add("Server: incorrect amount of command arguments.\n" +
                                "Use '/registration login password' to register " +
                                "or '/auth login password' to authorize.");
                    } else {
                        session.getServerMsgList().add(register(commandArgs[1], commandArgs[2]));
                    }
                } else if ("/auth".equals(commandArgs[0])) {
                    if (commandArgs.length != 3) {
                        session.getServerMsgList().add("Server: incorrect amount of command arguments.\n" +
                                "Use '/registration login password' to register " +
                                "or '/auth login password' to authorize.");
                    } else {
                        session.getServerMsgList().add(authorize(commandArgs[1], commandArgs[2]));
                    }
                } else {
                    session.getServerMsgList().add("Server: you are not in the chat!");
                }
                if (!session.getClientLogin().isEmpty()) {
                    session.getOnlineClients().add(session.getClientLogin());
                }
            } else {
                if (msg.startsWith("/")) {
                    if ("/exit".equals(msg)) {
                        session.setExit(true);
                    } else if ("/list".equals(msg)) {
                        if (session.getOnlineClients().size() == 1) {
                            session.getServerMsgList().add("Server: no one online");
                        } else {
                            StringBuilder onlineUsers = new StringBuilder("Server: online:");
                            synchronized (session.getOnlineClients()) {
                                Collections.sort(session.getOnlineClients());
                                for (String client : session.getOnlineClients()) {
                                    if (!session.getClientLogin().equals(client)) {
                                        onlineUsers.append(" ").append(client);
                                    }
                                }
                            }
                            session.getServerMsgList().add(onlineUsers.toString());
                        }
                    } else if ("/unread".equals(msg)) {
                        String users = getListWhoSentUnread();
                        if (users.isEmpty()) {
                            session.getServerMsgList().add("Server: no one unread");
                        } else {
                            session.getServerMsgList().add("Server: unread from:" + users);
                        }
                    } else if ("/stats".equals(msg)) {
                        String result = getStats();
                        session.getServerMsgList().add(result);
                    } else if (msg.startsWith("/chat")) {
                        String result = makeChat(msg);
                        if (!"ok".equals(result)) {
                            session.getServerMsgList().add(result);
                        }
                    } else if (msg.startsWith("/grant")) {
                        if ("admin".equals(session.getRole())) {
                            session.getServerMsgList().add(addModerator(msg));
                        } else {
                            session.getServerMsgList().add("Server: you are not an admin!");
                        }
                    } else if (msg.startsWith("/revoke")) {
                        if ("admin".equals(session.getRole())) {
                            session.getServerMsgList().add(removeModerator(msg));
                        } else {
                            session.getServerMsgList().add("Server: you are not an admin!");
                        }
                    } else if (msg.startsWith("/kick")) {
                        if ("admin".equals(session.getRole()) || "moderator".equals(session.getRole())) {
                            session.getServerMsgList().add(kick(msg));
                        } else {
                            session.getServerMsgList().add("Server: you are not a moderator or an admin!");
                        }
                    } else if (msg.startsWith("/history")) {
                        String result = getMsgHistory(msg);
                        session.getServerMsgList().add(result);
                    } else {
                        session.getServerMsgList().add("Server: incorrect command!");
                    }
                } else {
                    if (session.getAddresseeLogin().isEmpty()) {
                        session.getServerMsgList().add("Server: use /list command to choose a user to text!");
                    } else {
                        session.getChatRoom().getMsgList().add(new Message(session.getClientLogin(), msg));
                    }
                }
            }
        }
    }

    private String getStats() {
        int allMsgCounter = 0;
        int clientMsgCounter = 0;
        int addresseeMsgCounter = 0;
        try {
            Scanner scanner = new Scanner(session.getChatFile());
            while (scanner.hasNext()) {
                String msgAuthor = scanner.nextLine().split(": ")[0];
                if (msgAuthor.equals(session.getClientLogin())) {
                    clientMsgCounter++;
                } else {
                    addresseeMsgCounter++;
                }
                allMsgCounter++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "Server:\nStatistics with " + session.getAddresseeLogin() + ":\n" +
                "Total messages: " + allMsgCounter + "\n" +
                "Messages from " + session.getClientLogin() + ": " + clientMsgCounter + "\n" +
                "Messages from " + session.getAddresseeLogin() + ": " + addresseeMsgCounter;
    }

    private String getMsgHistory(String command) {
        if (session.getChatRoom().getType() == 0) {
            return "Server: you are not in a chat room!";
        }
        String[] commandArgs = command.trim().split("\\s+");
        if (commandArgs.length != 2) {
            return "Server: incorrect amount of arguments!\nUse '/history N' to get last N messages of this conversation.";
        }
        int numberOfMessages;
        try {
            numberOfMessages = Integer.parseInt(commandArgs[1]);
        } catch (NumberFormatException e) {
            return "Server: " + commandArgs[1] + " is not a number!";
        }
        if (numberOfMessages < 1) {
            return "Server: incorrect number of messages!";
        }
        Deque<String> msgFromFile = new ArrayDeque<>();
        int msgCounter = 0;
        try {
            Scanner msgFileScanner = new Scanner(session.getChatFile());
            while (msgFileScanner.hasNext()) {
                if (msgCounter == numberOfMessages) {
                    msgFromFile.poll();
                    msgCounter--;
                }
                msgFromFile.offer(msgFileScanner.nextLine());
                msgCounter++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuilder messages = new StringBuilder("Server:\n");
        int n = 0;
        if (msgCounter > 25) {
            msgCounter = 25;
        }
        for (int i = 0; i < msgCounter; i++) {
            messages.append(msgFromFile.poll());
            if (i < msgCounter - 1) {
                messages.append("\n");
            }
        }
        return messages.toString();
    }

    private String register(String login, String password) {
        if (session.getLogpassDB().containsKey(login)) {
            return "Server: this login is already taken! Choose another one.";
        }
        if (password.length() < 8) {
            return "Server: the password is too short!";
        }
        try {
            SyncDBFileWriter.write(login, password.hashCode(), "user", session.getUsersDBFilepath());
        } catch (IOException e) {
            e.printStackTrace();
            return "Server: an error occurred on the server side, try again.";
        }
        session.getLogpassDB().put(login, password.hashCode());
        session.setClientLogin(login);
        session.setRole("user");
        return "Server: you are registered successfully!";
    }

    private String authorize(String login, String password) {
        if (!session.getLogpassDB().containsKey(login)) {
            return "Server: incorrect login!";
        }
        if (!session.getLogpassDB().get(login).equals(password.hashCode())) {
            return "Server: incorrect password!";
        } else {
            if (!session.getKickedClients().containsKey(login)) {
                session.setClientLogin(login);
                if ("admin".equals(login)) {
                    session.setRole("admin");
                } else if (session.getModerators().contains(login)) {
                    session.setRole("moderator");
                } else {
                    session.setRole("user");
                }
                return "Server: you are authorized successfully!";
            } else {
                if (new Date().getTime() - session.getKickedClients().get(login).getTime() > (5000 * 60)) {
                    session.getKickedClients().remove(login);
                    session.setClientLogin(login);
                    if ("admin".equals(login)) {
                        session.setRole("admin");
                    } else if (session.getModerators().contains(login)) {
                        session.setRole("moderator");
                    } else {
                        session.setRole("user");
                    }
                    return "Server: you are authorized successfully!";
                } else {
                    return "Server: you are banned!";
                }
            }
        }
    }

    private String makeChat(String userCommand) {
        String[] commandArgs = userCommand.split("\\s+");
        if (commandArgs.length != 2) {
            return "Server: incorrect amount of arguments!\nUse '/chat name' to start chatting with user.";
        }
        if (session.getClientLogin().equals(commandArgs[1])) {
            return "Server: you can't chat with yourself!";
        }
        if (!session.getOnlineClients().contains(commandArgs[1])) {
            return "Server: the user is not online!";
        }
        if (session.getChatRoom().getType() == 1) {
            session.getChatRoom().getUsers().put(session.getClientLogin(), false);
        }
        session.setAddresseeLogin(commandArgs[1]);
        String chatID;
        if (session.getClientLogin().compareTo(session.getAddresseeLogin()) < 0) {
            chatID = session.getClientLogin() + "_" + session.getAddresseeLogin();
        } else {
            chatID = session.getAddresseeLogin() + "_" + session.getClientLogin();
        }
        File chatFile = new File("msgCache/" + chatID);
        try {
            chatFile.createNewFile();
            loadHistory(chatFile);
        } catch (IOException e) {
            e.printStackTrace();
            return "Server: an error occurred on the server side, try again.";
        }
        session.setChatFile(chatFile);
        if (!session.getChatRooms().containsKey(chatID)) {
            session.getChatRooms().put(chatID, new ChatRoom(session.getClientLogin(), session.getAddresseeLogin()));
        }
        session.setChatRoom(session.getChatRooms().get(chatID));
        session.getChatRoom().getUsers().put(session.getClientLogin(), true);
        return "ok";
    }

    private String getListWhoSentUnread() {
        File file = new File("msgCache/unreadUsers/" + session.getClientLogin());
        if (file.exists()) {
            StringBuilder users = new StringBuilder();
            try {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    users.append(line).append(" ");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String[] usersArr = users.toString().trim().split(" ");
            Arrays.sort(usersArr);
            users = new StringBuilder();
            for (String user : usersArr) {
                users.append(" ").append(user);
            }
            return users.toString();
        } else {
            return "";
        }
    }

    private String addModerator(String userCommand) {
        String[] commandArgs = userCommand.split("\\s+");
        if (commandArgs.length != 2) {
            return "Server: incorrect amount of arguments!\nUse '/grant name' to add new moderator.";
        }
        if (session.getClientLogin().equals(commandArgs[1])) {
            return "Server: you are admin!";
        }
        if (session.getModerators().contains(commandArgs[1])) {
            return "Server: this user is already a moderator!";
        }
        try {
            SyncDBFileWriter.changeRole(commandArgs[1], "moderator", session.getUsersDBFilepath());
        } catch (IOException e) {
            e.printStackTrace();
            return "Server: an error occurred on the server side, try again.";
        }
        synchronized (session.getOnlineSessions()) {
            for (Session session : session.getOnlineSessions()) {
                if (session.getClientLogin().equals(commandArgs[1])) {
                    session.setRole("moderator");
                    session.getServerMsgList().add("Server: you are the new moderator now!");
                    break;
                }
            }
        }
        session.getModerators().add(commandArgs[1]);
        return "Server: " + commandArgs[1] + " is the new moderator!";
    }

    private String removeModerator(String userCommand) {
        String[] commandArgs = userCommand.split("\\s+");
        if (commandArgs.length != 2) {
            return "Server: incorrect amount of arguments!\nUse '/revoke name' to revoke moderator rights.";
        }
        if (session.getClientLogin().equals(commandArgs[1])) {
            return "Server: you are admin!";
        }
        if (!session.getModerators().contains(commandArgs[1])) {
            return "Server: this user is not a moderator!";
        }
        try {
            SyncDBFileWriter.changeRole(commandArgs[1], "user", session.getUsersDBFilepath());
        } catch (IOException e) {
            e.printStackTrace();
            return "Server: an error occurred on the server side, try again.";
        }
        synchronized (session.getOnlineSessions()) {
            for (Session session : session.getOnlineSessions()) {
                if (session.getClientLogin().equals(commandArgs[1])) {
                    session.setRole("user");
                    session.getServerMsgList().add("Server: you are no longer a moderator!");
                    break;
                }
            }
        }
        session.getModerators().remove(commandArgs[1]);
        return "Server: " + commandArgs[1] + " is no longer a moderator!";
    }

    private String kick(String userCommand) {
        String[] commandArgs = userCommand.split("\\s+");
        if (commandArgs.length != 2) {
            return "Server: incorrect amount of arguments!\nUse '/kick name' to kick user.";
        }
        if (session.getClientLogin().equals(commandArgs[1])) {
            return "Server: you can't kick yourself!";
        }
        if ("moderator".equals(session.getRole()) && (session.getModerators().contains(commandArgs[1]) || "admin".equals(commandArgs[1]))) {
            return "Server: you can't kick other moderators!";
        }
        synchronized (session.getOnlineSessions()) {
            for (Session session : session.getOnlineSessions()) {
                if (session.getClientLogin().equals(commandArgs[1])) {
                    session.getServerMsgList().add("Server: you have been kicked out of the server!");
                    session.getServerMsgList().add("Server: authorize or register");
                    session.getKickedClients().put(session.getClientLogin(), new Date());
                    session.getChatRoom().getUsers().put(session.getClientLogin(), false);
                    session.setChatRoom(session.getChatRooms().get("guest"));
                    session.getOnlineClients().remove(session.getClientLogin());
                    session.setAddresseeLogin("");
                    session.setClientLogin("");
                    session.setRole("");
                    break;
                }
            }
        }
        return "Server: " + commandArgs[1] + " was kicked!";
    }

    private void loadHistory(File chatFile) throws IOException {
        File newMsgFile = new File(chatFile.getAbsolutePath() + "_new_" + session.getClientLogin());
        newMsgFile.createNewFile();
        Deque<String> allMsgFromFile = new ArrayDeque<>();
        List<String> newMsgFromFile = new ArrayList<>();
        Scanner newMsgFileScanner = new Scanner(newMsgFile);
        int newMsgCounter = 0;
        int allMsgCounter = 0;
        while (newMsgFileScanner.hasNext()) {
            newMsgFromFile.add(newMsgFileScanner.nextLine());
            newMsgCounter++;
        }
        Scanner allMsgFileScanner = new Scanner(chatFile);
        while (allMsgFileScanner.hasNext()) {
            if (allMsgCounter - newMsgCounter == 10) {
                allMsgFromFile.poll();
                allMsgCounter--;
            }
            allMsgFromFile.offer(allMsgFileScanner.nextLine());
            allMsgCounter++;
        }
        for (String msg : newMsgFromFile) {
            allMsgFromFile.removeLastOccurrence(msg);
        }
        newMsgFile.delete();
        if (!newMsgFromFile.isEmpty()) {
            for (int i = 0; i < newMsgFromFile.size(); i++) {
                newMsgFromFile.set(i, "(new) " + newMsgFromFile.get(i));
            }
            allMsgFromFile.addAll(newMsgFromFile);
        }
        while (allMsgFromFile.size() > 25) {
            allMsgFromFile.poll();
        }
        session.getHistoryMsgList().addAll(allMsgFromFile);
        removeUnreadUser();
    }

    private void removeUnreadUser() {
        File file = new File("msgCache/unreadUsers/" + session.getClientLogin());
        if (file.exists()) {
            StringBuilder users = new StringBuilder();
            try {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (!line.equals(session.getAddresseeLogin()) && !line.isEmpty()) {
                        users.append(line).append('\n');
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (users.toString().isEmpty()) {
                file.delete();
            } else {
                SyncMsgFileWriter.write(file, users.toString(), false);
            }
        }
    }
}

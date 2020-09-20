package chat.server;

import java.util.*;

public class ChatRoom {

    /**
     Keys of the map - logins, values - is user currently in this chatroom
     */
    private final Map<String, Boolean> users = new HashMap<>();
    private final List<Message> msgList = Collections.synchronizedList(new ArrayList<>());
    private final int type;

    public ChatRoom(String guest) {
        type = 0;
    }

    public ChatRoom(String creator, String addressee) {
        type = 1;
        users.put(creator, true);
        users.put(addressee, false);
    }

    public Map<String, Boolean> getUsers() {
        return users;
    }

    public List<Message> getMsgList() {
        return msgList;
    }

    public int getType() {
        return type;
    }
}

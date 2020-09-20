package chat.server.MessageOperations;

import chat.server.ChatRoom;
import java.util.Map;

public class MessageRemover extends Thread {
    private final Map<String, ChatRoom> chatRooms;

    public MessageRemover(Map<String, ChatRoom> chatRooms) {
        this.chatRooms = chatRooms;
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (chatRooms) {
                for (ChatRoom chatRoom : chatRooms.values()) {
                    if (!chatRoom.getMsgList().isEmpty()) {
                        synchronized (chatRoom.getMsgList()) {
                            chatRoom.getMsgList().removeIf(msg -> msg.isSavedToFile() && msg.isSentToAuthor() && msg.isSentToAddressee());
                        }
                    }
                }
            }
        }
    }
}

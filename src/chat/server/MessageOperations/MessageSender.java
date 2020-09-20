package chat.server.MessageOperations;

import chat.server.Message;
import chat.server.Session;

import java.io.DataOutputStream;
import java.io.IOException;

public class MessageSender extends Thread {

    private final Session session;
    private final DataOutputStream output;

    public MessageSender(Session session, DataOutputStream output) {
        this.session = session;
        this.output = output;
    }

    @Override
    public void run() {
        while (!session.isInterrupted() && !session.isExit()) {
            try {
                if (!session.getHistoryMsgList().isEmpty()) {
                    synchronized (session.getHistoryMsgList()) {
                        for (String msg : session.getHistoryMsgList()) {
                            output.writeUTF(msg);
                        }
                    }
                    session.getHistoryMsgList().clear();
                }
                if (!session.getServerMsgList().isEmpty()) {
                    for (String msg : session.getServerMsgList()) {
                        output.writeUTF(msg);
                        session.getServerMsgList().remove(msg);
                    }
                }
                if (!session.getChatRoom().getMsgList().isEmpty()) {
                    synchronized (session.getChatRoom().getMsgList()) {
                        for (Message msg : session.getChatRoom().getMsgList()) {
                            if (session.getClientLogin().equals(msg.getAuthor())) {
                                if (!msg.isSentToAuthor()) {
                                    output.writeUTF(msg.getText());
                                    msg.setSentToAuthor(true);
                                    if (!session.getChatRoom().getUsers().get(session.getAddresseeLogin())) {
                                        msg.setSentToAddressee(true);
                                    }
                                }
                            } else {
                                if (!msg.isSentToAddressee()) {
                                    output.writeUTF(msg.getText());
                                    msg.setSentToAddressee(true);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                break;
            }
        }
    }
}

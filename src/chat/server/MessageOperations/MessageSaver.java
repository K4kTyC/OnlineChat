package chat.server.MessageOperations;

import chat.server.Message;
import chat.server.Session;
import chat.server.SyncFileWriters.SyncMsgFileWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class MessageSaver extends Thread {

    private final Session session;

    public MessageSaver(Session session) {
        this.session = session;
    }

    @Override
    public void run() {
        while (!session.isInterrupted() && !session.isExit()) {
            if (!session.getChatRoom().getMsgList().isEmpty()) {
                synchronized (session.getChatRoom().getMsgList()) {
                    for (Message msg : session.getChatRoom().getMsgList()) {
                        if (!msg.isSavedToFile()) {
                            if (!session.getChatRoom().getUsers().get(session.getAddresseeLogin())) {
                                File file = new File(session.getChatFile().getAbsolutePath() + "_new_" + session.getAddresseeLogin());
                                try {
                                    file.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                SyncMsgFileWriter.write(file, msg.getText() + '\n', true);
                                addUnreadUser();
                            }
                            SyncMsgFileWriter.write(session.getChatFile(), msg.getText() + '\n', true);
                            msg.setSavedToFile(true);
                        }
                    }
                }
            }
        }
    }

    private void addUnreadUser() {
        File file = new File("msgCache/unreadUsers/" + session.getAddresseeLogin());
        boolean fileCreated;
        try {
            fileCreated = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (!fileCreated) {
            try {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.equals(session.getClientLogin())) {
                        return;
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        SyncMsgFileWriter.write(file, session.getClientLogin() + '\n', true);
    }
}

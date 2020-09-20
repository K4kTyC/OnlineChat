package chat.server.SyncFileWriters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SyncMsgFileWriter {
    public static synchronized void write(File file, String text, boolean append) {
        try (FileWriter msgDBFile = new FileWriter(file, append)) {
            msgDBFile.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

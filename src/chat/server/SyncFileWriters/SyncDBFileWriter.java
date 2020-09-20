package chat.server.SyncFileWriters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class SyncDBFileWriter {
    public static synchronized void write(String login, int password, String role, File usersDBFilepath) throws IOException {
        try (FileWriter usersDBFile = new FileWriter(usersDBFilepath, true)) {
            usersDBFile.write(login + " " + password + " " + role + '\n');
        }
    }

    public static synchronized void changeRole(String login, String newRole, File usersDBFilepath) throws IOException {
        Scanner scanner = new Scanner(usersDBFilepath);
        StringBuilder text = new StringBuilder();
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.startsWith(login + " ")) {
                String[] args = line.split(" ");
                text.append(args[0] + " " + args[1] + " " + newRole + '\n');
            } else {
                text.append(line + '\n');
            }
        }
        try (FileWriter usersDBFile = new FileWriter(usersDBFilepath)) {
            usersDBFile.write(text.toString());
        }
    }
}

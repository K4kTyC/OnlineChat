package chat.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
 
    private static boolean clientRunning;

    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 23456);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            clientRunning = true;
            System.out.println("Client started!");
            Scanner scanner = new Scanner(System.in);

            Thread fromConsoleToServer = new Thread(() -> {
                while (clientRunning) {
                    try {
                        String msg = scanner.nextLine();
                        output.writeUTF(msg);
                        if ("/exit".equals(msg)) {
                            clientRunning = false;
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
            });

            Thread fromServerToConsole = new Thread(() -> {
                while (clientRunning) {
                    try {
                        System.out.println(input.readUTF());
                    } catch (IOException e) {
                        break;
                    }
                }
            });

            fromConsoleToServer.start();
            fromServerToConsole.start();

            fromConsoleToServer.join();
            fromServerToConsole.join();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

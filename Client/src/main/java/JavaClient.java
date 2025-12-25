import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Java版本的客户端
 *
 * @author AfeiBaili
 * @version 2025/12/25 18:58
 */

public class JavaClient {
    private Socket socket;
    private JavaCipherProcessor cipher = new JavaCipherProcessor("thisatoken");
    private Thread readerThread;
    private Thread writerThread;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Scanner scanner = new Scanner(System.in);

    public void connect() {
        try {
            socket = new Socket("localhost", 33394);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("\n;" + cipher.decrypt(line));
                    }
                } catch (IOException e) {
                    System.out.println("连接关闭");
                }
            });

            writerThread = new Thread(() -> {
                while (!writerThread.isInterrupted()) {
                    System.out.print(":");
                    String line = scanner.nextLine();
                    writer.println(cipher.encrypt(line));
                }
            });
        } catch (IOException ignored) {
        }

        readerThread.start();
        writerThread.start();

        try {
            readerThread.join();
        } catch (Exception ignored) {
        }
    }

    public void close() {
        try {
            reader.close();
            writer.close();
            scanner.close();
            writerThread.interrupt();
            socket.close();
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        new JavaClient().connect();
    }
}
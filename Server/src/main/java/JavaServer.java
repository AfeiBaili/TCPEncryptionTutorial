import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * Java版本的Server
 *
 * @author AfeiBaili
 * @version 2025/12/25 18:17
 */

public class JavaServer {
    private ServerSocket serverSocket;
    private final HashMap<Socket, Tuple2<JavaReader, Writer>> socketMap = new HashMap<>();
    private Thread serverThread;

    {
        try {
            serverSocket = new ServerSocket(33394);
            serverSocket.setSoTimeout(1000);
        } catch (IOException ignored) {
        }
    }

    public void start() {
        serverThread = new Thread(() -> {
            System.out.println("服务端已开启");
            while (!serverThread.isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    socketMap.put(socket, new Tuple2<>(
                            new JavaReader(socket, "thisatoken"),
                            new Writer(socket, "thisatoken")
                    ));
                } catch (IOException ignored) {
                }
            }
        });

        serverThread.start();
        try {
            serverThread.join();
        } catch (InterruptedException ignored) {
        }
    }

    public void shutdown() {
        serverThread.interrupt();
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
        socketMap.forEach((Socket socket, Tuple2<JavaReader, Writer> tuple) -> {
            try {
                socket.close();
                tuple.first.close();
                tuple.second.close();
            } catch (IOException ignored) {
            }
        });
    }
}

class Tuple2<A, B> {
    A first;
    B second;

    public Tuple2(A first, B second) {
        this.first = first;
        this.second = second;
    }
}

class JavaReader {
    private final Thread readerThread;
    private BufferedReader reader;
    private CipherProcessor cipher;

    public JavaReader(Socket socket, String token) {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ignored) {
        }
        cipher = new CipherProcessor(token);

        readerThread = new Thread(() -> {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    System.out.println(":" + cipher.decrypt(line));
                }
            } catch (IOException ignored) {
                System.out.println("连接关闭");
            }
        });
        readerThread.start();
    }

    public void close() {
        try {
            reader.close();
        } catch (IOException ignored) {
        }
    }
}
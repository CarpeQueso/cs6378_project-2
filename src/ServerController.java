import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.ArrayList;


public class ServerController implements Runnable {

    private int port;

    private Queue<Message> messageQueue;

    private volatile boolean running;

    public ServerController(int port, Queue<Message> messageQueue) {
        this.port = port;
        this.messageQueue = messageQueue;
    }

    public void run() {
        running = true;

        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            while (running) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientConnectionManager(socket, messageQueue)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public void stop() {
        running = false;
    }
}

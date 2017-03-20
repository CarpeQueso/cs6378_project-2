import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.ArrayList;


public class ServerController implements Runnable {

    private int port;

    private Queue<Message> messageQueue;

	private ArrayList<ClientConnectionManager> clients;

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
				ClientConnectionManager client
					= new ClientConnectionManager(socket, messageQueue);
				clients.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public void stop() {
        running = false;
		for (ClientConnectionManager client : clients) {
			client.stop();
		}
    }
}

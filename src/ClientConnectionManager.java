import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Queue;


public class ClientConnectionManager implements Runnable {

	private Socket socket;

	private Queue<Message> messageQueue;

	private boolean shouldStop;

	public ClientConnectionManager(Socket socket, Queue<Message> messageQueue) {
		this.socket = socket;
		this.messageQueue = messageQueue;
		this.shouldStop = false;
	}

	public void run() {
		try {
			BufferedReader reader
				    = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			String messageString = reader.readLine();

			while (!shouldStop && messageString != null) {
				Message message = Message.parseMessage(messageString);
				if (message != null) {
					messageQueue.offer(message);
				}

				messageString = reader.readLine();
			}

			reader.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		this.shouldStop = true;
	}
}


public class Message {

	private MessageType type;

	private String body;

	public Message(MessageType type, String body) {
		this.type = type;
		this.body = body;
	}

	public MessageType getType() {
		return this.type;
	}

	public String getBody() {
		return this.body;
	}

	public String toString() {

		return null;
	}

	public static Message parseMessage(String messageString) {
		return null;
	}
}

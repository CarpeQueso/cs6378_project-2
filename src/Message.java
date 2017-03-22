
public class Message {

	private MessageType type;

	private int senderId;

	private String body;

	public Message(MessageType type, int senderId, String body) {
		this.type = type;
		this.senderId = senderId;
		this.body = body;
	}

	public MessageType getType() {
		return this.type;
	}

	public String getBody() {
		return this.body;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		switch (type) {
		case MAP:
			sb.append("MAP");
			break;
		case MARKER:
			sb.append("MARKER");
			break;
		case SNAPSHOT:
			sb.append("SNAPSHOT");
			break;
		case HALT:
			sb.append("HALT");
			break;
		default:
			sb.append("UNKNOWN");
		}
		sb.append(":");
		sb.append(senderId);
		sb.append(":");
		sb.append(body);
		
		return sb.toString();
	}

	public static Message parseMessage(String messageString) {
		String[] messageComponents = messageString.split(":");

		MessageType type;
		int senderId;
		String body;

		if (messageComponents[0].equals("MAP")) {
			type = MessageType.MAP;
		} else if (messageComponents[0].equals("MARKER")) {
			type = MessageType.MARKER;
		} else if (messageComponents[0].equals("SNAPSHOT")) {
			type = MessageType.SNAPSHOT;
		} else if (messageComponents[0].equals("HALT")) {
			type = MessageType.HALT;
		} else {
			type = MessageType.UNKNOWN;
		}

		senderId = Integer.parseInt(messageComponents[1]);
		body = messageComponents[2];
		
		return new Message(type, senderId, body);
	}
}



public class Neighbor {

	private final int id;

	private final String hostname;

	private final int port;

	public Neighbor(int id, String hostname, int port) {
		this.id = id;
		this.hostname = hostname;
		this.port = port;
	}

	public int getId() {
		return this.id;
	}

	public String getHostname() {
		return this.hostname;
	}

	public int getPort() {
		return this.port;
	}
}

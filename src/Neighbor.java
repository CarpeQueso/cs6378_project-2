import java.net.Socket;

public class Neighbor {

	private final int id;

	private final String hostname;

	private final int port;

	private boolean enabled;

	public Neighbor(int id, String hostname, int port) {
		this.id = id;
		this.hostname = hostname;
		this.port = port;

		this.enabled = true;
	}

	public void disable() {
		this.enabled = false;
	}

	public void enable() {
		this.enabled = true;
	}

	public boolean isEnabled() {
		return this.enabled;
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

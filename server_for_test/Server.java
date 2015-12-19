import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

	public static void main(String[] args) {
		new ServerThread().start();

	}
}

class ServerThread extends Thread {
	private static int Port = 8888;
	ServerSocket serversocket = null;

	public void run() {
		try {
			serversocket = new ServerSocket(Port);
			while (true) {
				Socket socket = serversocket.accept();
				BufferedReader buffer = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
		
				String msg = buffer.readLine();
				System.out.println("msg:" + msg);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				serversocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

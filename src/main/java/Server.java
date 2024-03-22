import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

	private ArrayList<ConnectionHandler> connections;
	private ServerSocket serverSocket;
	private boolean done;
	private ExecutorService pool;
	
	
	
	
	public Server() {
		connections = new ArrayList<>();
		done = false;
	}
	@Override
	public void run() {
		
		try {
		    serverSocket = new ServerSocket(9999);
		    pool = Executors.newCachedThreadPool();
		    while(!done) {
		    	Socket clientSocket = serverSocket.accept();
				ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket);
				connections.add(connectionHandler);
				pool.execute(connectionHandler);
		    }
			
		} catch (IOException e) {
			
			e.printStackTrace();
			shutDown();
		}
	}
	
	public void broadcast(String message) {
		for(ConnectionHandler ch : connections) {
			if(ch != null) {
				ch.sendMessage(message);
			}
		}
	}
	
	public void shutDown(){
		try {
			done = true;
			
			if(!serverSocket.isClosed()) {
				
					serverSocket.close();
			}
			for(ConnectionHandler ch : connections){
				ch.shutDown();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	
	class ConnectionHandler implements Runnable{
		
		private Socket clientSocket;
		private BufferedReader in;
		private PrintWriter out;
		private String nickname;
		public ConnectionHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}
		
		
		@Override
		public void run() {
			
			
			try {
				out = new PrintWriter(clientSocket.getOutputStream(),true);
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				out.println("please enter your nickname");
				nickname = in.readLine();
				System.out.println(nickname + " connected .");
				broadcast(nickname+ " joined the chat.");
				String message;
				while((message = in.readLine())!=null) {
					if(message.startsWith("/nick")) {
						String[] messageSplit = message.split(" ",2);
						if(messageSplit.length == 2) {
							broadcast(nickname + " renamed themselved to "+ messageSplit[1]);
							System.out.println(nickname + " renamed themselved to "+ messageSplit[1]);
							nickname = messageSplit[1];
							out.println("Succesfully changed nickname to " + nickname);
						}else {
							out.println("no nickname provided");
						}
					}else if(message.startsWith("/quit")){
						broadcast(nickname + " left the chat.");
						shutDown();
					}else {
						broadcast(nickname + " : " +message);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				shutDown();
			}
		}
		public void sendMessage(String message) {
			out.println(message);
		}
		
		public void shutDown() {
			try {
				in.close();
				out.close();
				
				if(!clientSocket.isClosed()) {
					clientSocket.close();
				}
			} catch (IOException e) {
				
			}
			
		}
	}
	
	public static void main(String[] args) {
		
		Server server = new Server();
		server.run();
		server.shutDown();
		
		
	}

}

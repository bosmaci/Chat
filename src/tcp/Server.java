package tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import utils.ChatUtils;

public class Server implements Runnable{
	
	private Thread t;
	private LinkedBlockingQueue<String> messages;
	private List<ServerCrawler> clients;
	private ServerSocket serverSocket;
	
	private static final String USER_MESSAGE_SEPARATOR = ";";
	private static final String USER_PRIVATE_MESSAGE_CLIENTS_SEPARATOR = ",";
	private static final String SERVER_MESSAGE = "server";
	
	private static final Logger logger = Logger.getLogger("Server");
	
	public Server() throws IOException {
		serverSocket = new ServerSocket(ChatUtils.DEFAULT_PORT);
		t = new Thread(this);
		messages = new LinkedBlockingQueue<String>();
		clients = new ArrayList<ServerCrawler>();
		t.start();
	}

	@Override
	public void run() {
		try {
			while(true){
				try {
					clients.add(new ServerCrawler(serverSocket.accept()));
					logger.info("connection successfull!");
					new MessagesDispathcer();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
	private class MessagesDispathcer implements Runnable{
		private Thread t;
		
		public MessagesDispathcer() {
			this.t = new Thread(this);
			this.t.setDaemon(true);
			this.t.start();
		}
		@Override
		public void run() {
			while(true){
				try {
					String messageToSend = messages.take();
					logger.info("Dispatching message -> " + messageToSend);
					sendMessageToAll(messageToSend);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}		
		}	
	}
	
	private void sendMessageToAll(String messageToSend){
		String[] data = messageToSend.split(USER_MESSAGE_SEPARATOR);
		List<ServerCrawler> toRemove = new ArrayList<ServerCrawler>();
		for (ServerCrawler serverCrawler : clients) {
			if(null == serverCrawler.socket){
				toRemove.add(serverCrawler);
				continue;
			}
			
			if(!serverCrawler.clientNickName.equals(data[0])){
				if(data.length == 3){
					if(isOnReceivers(data[1], serverCrawler.clientNickName)){
						sendMessageTo(serverCrawler,data[0], data[2]);
					}		
				} else {
					try {
						logger.info("sending message to " + serverCrawler.clientNickName);
						serverCrawler.sendMessageToClient(ChatUtils.currentTime() + data[0] + " -> " + data[1]);
					} catch (Exception e) {
						
					}
				}
				
			}
		}
		clients.removeAll(toRemove);
	}
	
	private boolean isOnReceivers(String receivers, String currentClient){
		try {
			String[] receiversArray = receivers.split(",");
			for (String receiver : receiversArray) {
				if(currentClient.equals(receiver.trim())){
					return true;
				}
			}
		} catch (Exception e) {
		}
		return false;
	}
	
	private void sendMessageTo(ServerCrawler serverCrawler, String sender, String message){
		try {
			serverCrawler.sendMessageToClient(ChatUtils.currentTime() + "private message from  " + sender+ " -> " + message);
			logger.info("sending private message to " + serverCrawler.clientNickName);
		} catch (IOException e) {
		}
		
	}
	
	private class ServerCrawler implements Runnable{
		private Logger serverCrawlerLogger;
		
		private BufferedReader inFromClient;
		private BufferedWriter outToClient;
		private Thread t;
		private Socket socket;
		private String clientNickName;
		
		private ServerCrawler(Socket connectionSocket) throws IOException {
			socket = connectionSocket;
			inFromClient = ChatUtils.getBufferedReader(socket.getInputStream());
			outToClient = ChatUtils.getBufferedWriter(socket.getOutputStream());
			t = new Thread(this);
			t.setDaemon(true);
			serverCrawlerLogger = Logger.getLogger("ServerCrawler_"+t.getName());
			
			t.start();
		}
		
		@Override
		public void run() {
			String messageFromClient = null;
			try {
				addClient();
				do{
					messageFromClient = ChatUtils.readFromSocket(inFromClient);
					logger.info("in From Client -> " + messageFromClient);
					enqueueMessageReceived(messageFromClient);
				} while(!ChatUtils.END_CHAT_KEYWORD.equalsIgnoreCase(messageFromClient) && !"".equals(messageFromClient));
			} catch (Exception e) {
				e.printStackTrace();
				try {
					socket.close();
				} catch (IOException e1) {
				} finally{
					enqueueMessageReceived(ChatUtils.END_CHAT_KEYWORD);
					socket = null;
				}
			}	
		}
		
		private void addClient() throws IOException{
			this.clientNickName = ChatUtils.readFromSocket(inFromClient);//FIXME risolvere omonimia
			ChatUtils.writeToSocket(outToClient, "WELCOME BACK " + clientNickName + "!");
			messages.add(SERVER_MESSAGE + USER_MESSAGE_SEPARATOR + clientNickName + " joined chat!");
		}
		
		private void enqueueMessageReceived(String message){
			if(!ChatUtils.END_CHAT_KEYWORD.equalsIgnoreCase(message) && !"".equals(message)){
				if(message.contains(ChatUtils.COMMANDS.SENDTO.getCommand())){
					try {
						messages.add(composePrivateMessage(message));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					messages.add(clientNickName + USER_MESSAGE_SEPARATOR + message);
				}
			} else {
				messages.add(clientNickName + USER_MESSAGE_SEPARATOR + clientNickName + " has quit chat!");
			}
		}
		
		private void sendMessageToClient(String message) throws IOException{
			int i = 0;
			while(i < ChatUtils.MAX_ATTEMPS_WAITING_USER_CONNECTION){
				try {
					ChatUtils.writeToSocket(outToClient, message);
					break;
				} catch (Exception e) {
					i++;
				}
			}
			
		}
		
		private String composePrivateMessage(String message) throws Exception{
			try {
				String[] messageParts = message.split(ChatUtils.COMMANDS.SENDTO.getCommand());
				return this.clientNickName + USER_MESSAGE_SEPARATOR + messageParts[1] + USER_MESSAGE_SEPARATOR + messageParts[0];
			} catch (Exception e) {
				throw e;
			}
			
		}
		
	}

	
	public static void main(String[] args) throws IOException {
		Server s = new Server();
	}

}

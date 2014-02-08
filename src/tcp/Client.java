package tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.ChatUtils;

public class Client implements Runnable{
	
	private Thread t;
	private Socket toClientSocket;
	private BufferedReader inFromConsole;
	private BufferedWriter outToConsole;
	
	private BufferedWriter outToServer;
	
	private String nickName;
	
	private static final Logger logger = Logger.getLogger("Client");
	
	public Client(String serverHostOrIpAddress, int serverPort, String nickName) throws UnknownHostException, IOException {
		logger.setLevel(Level.WARNING);
		toClientSocket = new Socket(serverHostOrIpAddress, serverPort);
		inFromConsole = ChatUtils.getBufferedReader(System.in);
		outToConsole = ChatUtils.getBufferedWriter(System.out);
		outToServer = ChatUtils.getBufferedWriter(toClientSocket.getOutputStream());
		this.nickName = nickName;
		t = new Thread(this);
		t.start();
	}

	@Override
	public void run() {
		try {
			new ServerMessagesReader();
			
			identifyMyselfToServer();
			
			String messageFromConsole;
			do{
				messageFromConsole = ChatUtils.readFromConsole(inFromConsole);
				if(!ChatUtils.HELP_MESSAGE.equalsIgnoreCase(messageFromConsole)){
					ChatUtils.writeToSocket(outToServer, messageFromConsole);
					logger.info("out To Server -> " + messageFromConsole);
				} else {
					ChatUtils.writeToConsole(outToConsole,messageFromConsole);
				}
			} while(!ChatUtils.END_CHAT_KEYWORD.equalsIgnoreCase(messageFromConsole));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				toClientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
	
	private synchronized void identifyMyselfToServer() throws Exception{
		int i = 0;
		while(i < ChatUtils.MAX_ATTEMPS_WAITING_USER_CONNECTION){
			try {
				ChatUtils.writeToSocket(outToServer, nickName);
				break;
			} catch (Exception e) {
				Thread.sleep(1000);
				i++;
				if(i==ChatUtils.MAX_ATTEMPS_WAITING_USER_CONNECTION){
					throw e;
				}
			}
		}
		
	}
	
	private class ServerMessagesReader implements Runnable{
		
		private Thread t;
		private BufferedReader inFromServer;
		
		public ServerMessagesReader() throws UnsupportedEncodingException, IOException {
			inFromServer = ChatUtils.getBufferedReader(toClientSocket.getInputStream());
			this.t = new Thread(this);
			this.t.setDaemon(true);
			this.t.start();
		}
		@Override
		public void run() {
			String messageFromServer = null;
			do{
				try {
					messageFromServer = ChatUtils.readFromSocket(inFromServer);
					logger.info("in From Server -> " + messageFromServer);
					ChatUtils.writeToConsole(outToConsole, messageFromServer);
				} catch (IOException e) {
					e.printStackTrace();
					messageFromServer = null;
				}
			}while(null != messageFromServer && !"".equals(messageFromServer));
			try {
				toClientSocket.close();
			} catch (IOException e) {
				toClientSocket = null;
			}
		}
		
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		Properties prop = new Properties();
		prop.load(Client.class.getClassLoader().getResourceAsStream("config/chat.properties"));
		String serverHostOrIpAddress = prop.getProperty("server.host");
		Integer serverPort = null;
		try {
			serverPort = Integer.parseInt(prop.getProperty("server.port"));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		String nickName = prop.getProperty("nickname");
		if(serverHostOrIpAddress.isEmpty() || serverPort == null || nickName.isEmpty()){
			System.exit(0);
		}
		Client c = new Client(serverHostOrIpAddress, serverPort, nickName);
	}

}

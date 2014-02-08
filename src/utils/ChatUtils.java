package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatUtils {
	
	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
	public static final int DEFAULT_PORT = 6789;
	public static final char DEFAULT_MESSAGE_DELIMITER = '\n';
	public static final int MAX_ATTEMPS_WAITING_USER_CONNECTION = 5;
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("[dd/MM/yy] [hh:mm:ss] ");
	
	public static final String END_CHAT_KEYWORD = "EXIT";
	public static final String END_CHAT_BYE_BYE_MESSAGE= "Bye bye!";
	public static final String HELP_MESSAGE = "Commands available:\n\n  EXIT or exit \"Quit chat!\"\n  your message --sendTo=client1,... \"Send private message to specified clients!\"\n\n";
	
	public enum COMMANDS{
		HELP("help"),
		SENDTO("--sendTo=");
		
		private String command;
		private COMMANDS(String s) {
			command = s;
		}
		
		public String getCommand() {
			return command;
		}
	}
	
	public static BufferedReader getBufferedReader(InputStream inputStream) throws UnsupportedEncodingException{
		return new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CHARSET));
	}
	
	public static BufferedWriter getBufferedWriter(OutputStream outputStream) throws UnsupportedEncodingException{
		return new BufferedWriter(new OutputStreamWriter(outputStream, DEFAULT_CHARSET));
	}
	
	public static String readFromSocket(BufferedReader reader) throws IOException{
		StringBuffer toRet = new StringBuffer();
		int chr;
		while((chr = reader.read()) != -1){
			char currentChar = (char)chr;
			if(currentChar == DEFAULT_MESSAGE_DELIMITER){
				break;			
			}
			toRet.append(currentChar);
		}
		
		return toRet.toString();
	}
	
	public static void writeToSocket(BufferedWriter writer, String message) throws IOException{
		int messageLength = message != null ? message.length() : 0;
		for (int i = 0; i < messageLength; i++) {
			writer.write(message.charAt(i));
		}

		writer.write(DEFAULT_MESSAGE_DELIMITER);
		writer.flush();
	}
	
	public static String readFromConsole(BufferedReader reader) throws IOException{
		String currentLine = reader.readLine();	
		if(currentLine.isEmpty()){
			readFromConsole(reader);
		} else {
			if(COMMANDS.HELP.getCommand().equalsIgnoreCase(currentLine)){
				return HELP_MESSAGE;
			}
			
			if(currentLine.contains(COMMANDS.SENDTO.getCommand())){
				if(!currentLine.trim().matches("^.+\\s"+COMMANDS.SENDTO.getCommand()+"(.+(,)*)+$")){
					return HELP_MESSAGE;
				}
			}
		}
		return currentLine.trim();
	}
	
	public static void writeToConsole(BufferedWriter writer, String message) throws IOException{
		writeToSocket(writer, message);
	}
	
	public static String currentTime(){
		String now = "";
		try {
			now = dateFormat.format(new Date(System.currentTimeMillis()));
		} catch (Exception e) {
		}
		return now;
	}

}

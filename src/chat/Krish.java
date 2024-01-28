package chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;



public class Krish{
	static Logger logger = Logger.getLogger(Krish.class);
	public static Connection connection;
	ServerSocket serverSocket;
	Socket socket;
	DataInputStream dis;
	DataOutputStream dos;
	static Map<String, Clients> availClients = new HashMap<String, Clients>();
	
	Inet4Address ipAddress;
	
	public Krish() {
		PropertyConfigurator.configure("log4.properties");
		
		try {
	        Class.forName("com.mysql.cj.jdbc.Driver");
	        connection = DriverManager.getConnection(
	                "jdbc:mysql://localhost:3306/chatApp", "root", ""
	        );
	        
	    } catch (SQLException e) {
	        logger.fatal("JDBC connection failed");
	    } catch (ClassNotFoundException e) {
			logger.fatal("Sql connector driver not found!");
		}
		
		System.out.println("***************************************");
		
		try {
			ipAddress = (Inet4Address) Inet4Address.getLocalHost();
			System.out.println("Server IP: "+ipAddress);
			
		} catch (UnknownHostException e) {
			System.out.println("Can't connect to this port!");
		}
		
		
		try {
			serverSocket = new ServerSocket(4455);
			logger.info("Server Started @ "+ipAddress);
			
			// Accept every client
			while (true) {
				socket = serverSocket.accept();
				logger.info("New handshake!");
				String ipString = socket.getInetAddress().toString().replace('/', '@');
				System.out.println(ipString);
				
				// get Client name;
				dis = new DataInputStream(socket.getInputStream());
				String cliName = dis.readUTF();
				Clients newCli = new Clients(socket,cliName);
				
				// update client in DB
				updateClient(ipString, cliName);
				logger.info("new client connected! "+cliName+ipString);
				// announce to all
				Krish.announce(cliName + newCli.ipAddress+" joined!");
				
				// save new client in hashmap!
				availClients.put(ipString, newCli);
				newCli.start(); // start new thread
			}
			
		} catch (Exception e) {
			System.out.println("Port connection failed");
			logger.fatal("Can't create server @ "+ipAddress+":"+4455);
		}
		
	}
	
	
	public static void main(String[] args) {
		new Krish();
		
	}

	public static String getAvailClients() {
		String availCli = "Availiable Clients: \n";
		for (Map.Entry<String, Clients> entry : availClients.entrySet()) {
			String ip = entry.getKey();
			String name = entry.getValue().Name;
			availCli += name + ip + "\n";
		}
		return availCli;
	}
	
	
	// announce new arrival to all
	public static void announce(String msg) {
		for (Map.Entry<String, Clients> entry : availClients.entrySet()) {
			entry.getValue().sendMessage(msg);
		}
	}
	
	
	
	// Update new client on clients table
	public static void updateClient(String clientID, String name) {

        String sql = "INSERT INTO clients (clientID, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setString(1, clientID);
            stmt.setString(2, name);

            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
            	logger.info("Client updated successfully!"+ affectedRows);
            } else {
               logger.error("client updation failed!"+ affectedRows);
            }

        } catch (SQLException e) {
            logger.error("Sql connection failed");
        }
	}


	// insert a message on message table
	public static void insertMessage(String senderId, String receiverId, String messageText) {
        
        String sql = "INSERT INTO Messages (SenderID, ReceiverID, MessageText) VALUES (?, ?, ?)";

        
           try(PreparedStatement stmt = connection.prepareStatement(sql)){
            
            
            stmt.setString(1, senderId);
            stmt.setString(2, receiverId);
            stmt.setString(3, messageText);

            
            int affectedRows = stmt.executeUpdate();

            
            if (affectedRows < 1) {
            	logger.error("Message insertion failed!"+affectedRows);
            } 
       
           } catch (Exception e) {
        	   logger.error("Can't reach database!");
		}
	}
	
	
	//
	
	public static Map<Date, String> fetchMessages(String senderID, String receiverID) {
        // TreeMap to store messages sorted by timestamp
        Map<Date, String> messages = new TreeMap<>();

        String sql1 = "SELECT SenderID, MessageText, Timestamp FROM Messages WHERE (SenderID = ? AND ReceiverID = ?) OR (SenderID = ? AND ReceiverID = ?) ORDER BY Timestamp";

        try (PreparedStatement pstmt = connection.prepareStatement(sql1)) {

            pstmt.setString(1, senderID);
            pstmt.setString(2, receiverID);
            pstmt.setString(3, receiverID);
            pstmt.setString(4, senderID);
            // TO get send msgs as well as received msgs
            
            try (ResultSet results = pstmt.executeQuery()) {

                while (results.next()) {
                    String sender = results.getString("SenderID");
                    String messageText = results.getString("MessageText");
                    Date timestamp =  results.getDate("Timestamp");

                    
                    String formattedMsg = sender.equals(senderID) ? "You: " + messageText : Krish.availClients.get(sender).Name + ": " + messageText;

                    // Add messages to TreeMap
                    messages.put(timestamp, formattedMsg);
                }
                
                logger.info("Messages recovered: "+ messages.size());
            }
        } catch (SQLException e) {
            System.out.println("Database connection failed");
            logger.error("DB connection failed while getting messages: "+ senderID+"-"+receiverID);
        } 

        return messages;
    }




}

class Clients extends Thread{
	private Socket socket;
	String Name;
	DataInputStream dis;
	DataOutputStream dos;
	String ipAddress;
	boolean isAvail;
	boolean wantTostop;
	Clients anotherPerson;
	static Logger logger = Logger.getLogger(Clients.class);
	Scanner sc = new Scanner(System.in);
	
	public Clients(Socket cli, String name) throws IOException {
		PropertyConfigurator.configure("log4.properties");
		this.socket = cli;
		this.Name = name;
		this.ipAddress = cli.getInetAddress().toString().replace('/', '@');
		this.dos = new DataOutputStream(cli.getOutputStream());
		this.anotherPerson=null;
	}
	
	public void sendMessage(String msg) {
		try {
			this.dos.writeUTF(msg);
		} catch (IOException e) {
			logger.error("Error on write message!");
		}
		
	}

	public void createConnection(Clients anotherCli) {
		this.anotherPerson = anotherCli;
		logger.info("One-One connection established! "+this.ipAddress+"-"+anotherPerson.ipAddress);
	}

	@Override
	public void run() {


			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				//
			}
			
			try {
				dis = new DataInputStream(this.socket.getInputStream());
			} catch (IOException e) {
				logger.error("Can't get Inputstream from "+ this.ipAddress);
			}
			
			
				// show avail clients
				try {
					dos.writeUTF(Krish.getAvailClients());
				} catch (IOException e) {
					logger.error("Can't send availiable clients to "+this.ipAddress);
				}
				String Clientip;

				// loop until get one-one connection
				while (this.anotherPerson == null) {
					try {
						Clientip = dis.readUTF();
						if (Krish.availClients.get(Clientip)!=null) {
							createConnection(Krish.availClients.get(Clientip));
							break;
						}
					} catch (Exception e) {
						logger.warn("Client left without connect: "+this.ipAddress);
						Krish.availClients.remove(this.ipAddress);
						this.stop();
					}
					
				}
			
			// import previous conversations

			Map<Date, String> recoveredMsgs = Krish.fetchMessages(this.ipAddress, this.anotherPerson.ipAddress);
			String toImport="\n";
			if (recoveredMsgs.size() > 0) {
				for (Map.Entry<Date, String> entry : recoveredMsgs.entrySet()) {
					
					toImport += entry.getValue()+"\n";
					
				}
			}
			try {
				this.dos.writeUTF(toImport);
			} catch (IOException e) {
				logger.error("Can't import previous conversation! to: "+this.ipAddress);
			}

			while (true){
				try {
					String msg = dis.readUTF();
					Krish.insertMessage(this.ipAddress, anotherPerson.ipAddress, msg);
					this.anotherPerson.sendMessage(this.Name+": "+msg);
				} catch (Exception e) {
					logger.warn("One-One connection closed: "+this.ipAddress+"-"+anotherPerson.ipAddress);
					Krish.availClients.remove(this.ipAddress);
					Krish.availClients.remove(anotherPerson.ipAddress);
					break;
				}			
			}
			

	}

}


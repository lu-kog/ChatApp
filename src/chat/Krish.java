package chat;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class Krish extends Frame implements ActionListener{
	static Logger logger = Logger.getLogger(Krish.class);
	
	TextField txtField;
	static TextArea txtArea;
	Button sendButton;
	
	ServerSocket serverSocket;
	Socket socket;
	
	DataInputStream dis;
	DataOutputStream dos;
	static Set<DataOutputStream> dosList = new HashSet<DataOutputStream>();
	
	Inet4Address ipAddress;
	
	public Krish() {
		PropertyConfigurator.configure("log4.properties");
		txtArea = new TextArea();
		txtField = new TextField(20);
		sendButton = new Button("Send");
	    
		txtArea.setBounds(50, 50, 400, 250);
        txtField.setBounds(50, 350, 400, 30);
		
		add(txtArea);
		add(txtField);
		add(sendButton);
		
		sendButton.addActionListener(this);
		
		setSize(500,500);
		setTitle("Krish");
		setVisible(true);
		
		// To close the window
        addWindowListener((WindowListener) new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
            	logger.fatal("Server Closed!");
                System.exit(0);
            }
        });
        logger.info("Servers Frame Created");
        
		
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
				dos = new DataOutputStream(socket.getOutputStream());
				dosList.add(dos);
				ClientThread newCli = new ClientThread(socket,dos);
				newCli.start();
			}
			
		} catch (Exception e) {
			System.out.println("Port connection failed");
			logger.error("Can't create server @ "+ipAddress+":"+4455);
		}
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String msg = txtField.getText();
		txtArea.append("You: "+msg+"\n");
		txtField.setText("");
		
		dosList.forEach(x->
		{
			try {
				x.writeUTF("Krish: "+msg);
			} catch (IOException e1) {
				System.out.println("Can't Reach!");
				logger.error("Can't send msg for to:"+x.toString());
			}
		});
		logger.info("Message broadcasted to every client.");
		
	}
	
	public static void main(String[] args) {
		new Krish();
		
	}

	
	

}

class ClientThread extends Thread{
	static Logger logger = Logger.getLogger(ClientThread.class);
	private Socket client;
	DataInputStream dis;
	DataOutputStream dos;
	
	public ClientThread(Socket cli, DataOutputStream dos) {
		this.client = cli;
		this.dos = dos;
		PropertyConfigurator.configure("log4.properties");
	}
	
	@Override
	public void run() {

		try {
			DataInputStream dis = new DataInputStream(client.getInputStream());
			
			// Receive client name
			String cliName = dis.readUTF();
			logger.info("New client connected!"+" Name: "+cliName);
			while (true){
				try {
					String msg = dis.readUTF();
					Krish.txtArea.append(cliName+": "+msg+"\n");
					logger.info("Message received!");
					// Broadcast to everyone
					
					for (Iterator<DataOutputStream> itr=Krish.dosList.iterator(); itr.hasNext() ;) {
						DataOutputStream currentDos = itr.next();
						if (currentDos.equals(dos)) continue;
						try {
							currentDos.writeUTF(cliName+": "+msg);
						} catch (IOException e1) {
							System.out.println("Can't Reach!");
						}
					}
					logger.info("Message forwarded to everyone!");
					
					
				} catch (Exception e) {
					System.out.println("Client left!");
					logger.warn("Client left!");
					Krish.dosList.remove(dos);
					break;
				}			
			}
		} catch (Exception e) {
			System.out.println("Thread Closed");
		}
		

	}
	
}

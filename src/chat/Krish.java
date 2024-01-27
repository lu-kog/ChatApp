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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
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
//	static Set<DataOutputStream> dosList = new HashSet<DataOutputStream>();
	static Map<String, Clients> availClients = new HashMap<String, Clients>();
	
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
				System.out.println();
				String ipString = socket.getInetAddress().toString().replace('/', '@');
				System.out.println(ipString);
				
				// get Client name;
				dis = new DataInputStream(socket.getInputStream());
				String cliName = dis.readUTF();
				Clients newCli = new Clients(socket,cliName);
				
				// save new client along with ip!
				availClients.put(ipString, newCli);
				newCli.start();
			}
			
		} catch (Exception e) {
			System.out.println("Port connection failed");
			logger.error("Can't create server @ "+ipAddress+":"+4455);
		}
		
	}
	
	
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) {
        	String msg = txtField.getText();
    		txtArea.append("You: "+msg+"\n");
    		txtField.setText("");
        } else if (e.getSource() == connectButton) {
            String ipaddr = txtField.getText();
            txtArea.append("Connecting to "+ipaddr);
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

	
	

}

class Clients extends Thread{
	private Socket socket;
	String Name;
	DataInputStream dis;
	DataOutputStream dos;
	String ipAddress;
	boolean isAvail;
	boolean wantTostop;
	OnetoOne onetoOne;
	Clients anotherPerson;
	Scanner sc = new Scanner(System.in);
	
	public Clients(Socket cli, String name) throws IOException {
		this.socket = cli;
		this.Name = name;
		this.ipAddress = cli.getInetAddress().toString().replace('/', '@');
		this.isAvail = true;
		this.dos = new DataOutputStream(cli.getOutputStream());
	}
	
	public void createConnection(Clients anotherCli) {
//		onetoOne = new OnetoOne(this.socket, anotherCli.socket, this.ipAddress, anotherCli.ipAddress);
//		onetoOne.start();
//		
//		try {
//			onetoOne.join();
//		} catch (InterruptedException e) {
//			//
//		}
		
	}
	
	@Override
	public void run() {

		try {
			Thread.sleep(2000);
			dis = new DataInputStream(socket.getInputStream());
			dos.writeUTF(Krish.availClients.size()+"");
			if (Krish.availClients.size()>1) {
				// show avail clients 
				dos.writeUTF(Krish.getAvailClients());
				String getClientip = dis.readUTF();
				this.isAvail = false;
				Krish.availClients.get(getClientip).isAvail=false;
				Krish.availClients.get(getClientip).createConnection(Krish.availClients.get(this.ipAddress));
				this.createConnection(Krish.availClients.get(getClientip));
			}
			
			while (Krish.availClients.get(ipAddress).isAvail){
				try {
					dos.writeUTF("Select a Ip: ");
					String msg = dis.readUTF();
					if (Krish.availClients.get("@"+msg) != null) {
						String getClientip = msg;
						this.isAvail = false;
						Krish.availClients.get(getClientip).isAvail=false;
						Krish.availClients.get(getClientip).createConnection(Krish.availClients.get(this.ipAddress));
						this.createConnection(Krish.availClients.get(getClientip));
					}
					System.out.println(Name+": suthudhu..");
				} catch (Exception e) {
					System.out.println("Client left!");
					break;
				}			
			}
			
			onetoOne.join();
			
			
			System.out.println(this.Name+" "+"Sethuttan..");
		} catch (Exception e) {
			System.out.println("Thread Closed");
		}

	}

}

class OnetoOne extends Thread{
	Socket clientA;
	Socket clientB;
	String clientAname;
	String clientBname;
	String cliAip;
	String cliBip;

	public OnetoOne(Socket a, Socket b, String cliAName, String cliBName) {
		this.clientA = a;
		this.clientB = b;
		this.clientAname = cliAName;
		this.clientBname = cliBName;
		this.cliAip = clientA.getInetAddress().toString().replace('/', '@');
		this.cliBip = clientB.getInetAddress().toString().replace('/', '@');
	}

	
	@Override
	public void run() {
		try {
			Krish.availClients.get(cliAip).wantTostop = false;			
			
			DataInputStream dis = new DataInputStream(clientA.getInputStream());
			DataOutputStream dos = new DataOutputStream(clientB.getOutputStream());
			Thread.sleep(3000);
			dos.writeUTF("**********");
			dos.writeUTF(clientBname);
			dos.writeUTF(cliBip);
			dos.writeUTF("***********");
			String nameString = Krish.availClients.get(cliAip).Name;

			
			System.out.println(Krish.availClients.size());
			System.out.println();
			
			while (true){
				try {
					String msg = dis.readUTF();
					dos.writeUTF(nameString+": "+msg);
			
				} catch (Exception e) {
					System.out.println("Client left!");
					break;
				}			
			}
			
			System.out.println("******w***********************");
			
		} catch (IOException e) {
			System.out.println("*******a**********************");
		} catch (InterruptedException e1) {
			System.out.println("*********b********************");
		}
		
	}
	
	
}
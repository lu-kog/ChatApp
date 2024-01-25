package chat;

import java.awt.Button;
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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client extends Frame implements ActionListener{
	TextField txtField;
	TextArea txtArea;
	Button sendButton;
	
	Socket socket;
	DataInputStream dis;
	DataOutputStream dos;
	
	Inet4Address ipAddress;
	
	String cliName;
	
	public Client() {
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter Your Name: ");
		cliName = sc.nextLine();
		
		createFrame();
		// To close the window
        addWindowListener((WindowListener) new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });
        
		
		try {
			byte[] addrs = {(byte) 172, 17, 55, (byte) 207};
			ipAddress = (Inet4Address) InetAddress.getByAddress(addrs);
			System.out.println("Server IP: "+ ipAddress.getHostAddress());
            
		} catch (UnknownHostException e) {
	            System.out.println("Can't Connect host");
		}
		
		
		// Create client socket
		try {
			socket = new Socket(ipAddress, 4455);
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			
			dos.writeUTF(cliName);
			
		} catch (Exception e) {
			System.out.println("Port connection failed");
		}
		
		while (true){
			try {
				String msg = dis.readUTF();
				txtArea.append(msg+"\n");
				
			} catch (Exception e) {
				System.out.println("Thread Closed");
				break;
			}
			
		}
		
		System.exit(0);
		
		
	}
	
	
	// creating a chat window using frame
	public void createFrame() {
		txtArea = new TextArea();
		txtField = new TextField();
		sendButton = new Button("Send");
		
		sendButton.addActionListener(this);
		
		txtArea.setBounds(50, 50, 400, 250);
        txtField.setBounds(50, 350, 400, 30);

		
		add(txtArea);
		add(txtField);
		add(sendButton);
		
		
		setSize(500,500);
		setTitle("You");
		setVisible(true);
	}
	
	
	
	public static void main(String[] args) {
		
		new Client();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String msg = txtField.getText();
		txtArea.append(cliName+": "+msg+"\n");
		txtField.setText("");
		
		try {
			dos.writeUTF(msg);
			dos.flush();
		} catch (IOException e1) {
			System.out.println("Error on send msg!");
		}
		
		
	}


}

package application;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

import client.Client;
import client.TokenHandling;


public class App {
	private HashMap<String, Client> clientMap;
	private Client currentClient;

	public App() {
		clientMap = new HashMap<String, Client>();
		currentClient = null;
	}

	public void start() {
		Scanner scanner  = new Scanner(System.in);

		while(true) {
			System.out.print(">");
			String[] input = scanner.nextLine().split(" +");
			
			if(input[0].equals("quit")){
				if(input.length >= 2 && input[1].equals("-l")) {
					for(Client cur : clientMap.values()) {
						cur.logout();
					}
				}
				break;
			} else if(input[0].equals("help")) {
				
			} else if(input[0].equals("reconnect")) {
				try {
					File test = new File("./69347337-user");
					
					// Get credentials from file and connect user.
					String username = test.getName().split("-")[0];
					String access_token = TokenHandling.getFromPKCS12KeyStore("Land2070", test.getName(), "access_token");
					String refresh_token = TokenHandling.getFromPKCS12KeyStore("Land2070", test.getName(), "refresh_token");
					String request_id = TokenHandling.getFromPKCS12KeyStore("Land2070", test.getName(), "request_id").split("-")[0];
					String session_id = TokenHandling.getFromPKCS12KeyStore("Land2070", test.getName(), "session_id");


					Client next = new Client(username, 
					access_token, refresh_token, request_id, session_id);
					clientMap.put(username, next);
					currentClient = next;
				} catch (Exception e) {
					// TODO: handle exception
				}
			} else if(input.length == 2 && input[0].equals("connect")) {
				// Go through directory and connect matching user.
				if(clientMap.get(input[1]) == null) {
					boolean connected = false; 
					File dir = new File("./");
					File[] dirList = dir.listFiles();

					for(File cur : dirList) {
						String[] name  = cur.getName().split("-");

						if(input[1].equals("-a") || input[1].equals(name[0])) {
							connected = true;
						}

						if(name.length == 2 && name[1].equals("user") && connected){
							try {
								// Get credentials from file and connect user.
								String password = TokenHandling.getFromPKCS12KeyStore("Land2070", cur.getName(), "password").split("-")[0];
								String client_id = "User_" + TokenHandling.getFromPKCS12KeyStore("Land2070", cur.getName(), "client_id");
								String client_secret = TokenHandling.getFromPKCS12KeyStore("Land2070", cur.getName(), "client_secret");
								
								// Connect client.
								Client next = new Client(name[0]);
								next.connect(password, client_id, client_secret);

								System.out.println(">>Successfully connected to client " + name[0] + ".");
								clientMap.put(name[0], next);
								currentClient = next;
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					if(!connected) {
						System.out.println(">>No such user found. Try new <username> <password> <client_id> <client_secret>");
					}
				} else {
					System.out.println(">>User already connected.");
				}

			} else if(input[0].equals("new")) {
				if(input.length == 5) {
					
					// Check if user is already connected.
					if(clientMap.get(input[1]) == null) {
						try {
							Client next = new Client(input[1]);
							next.connect(input[2], input[3], input[4]);

							System.out.println(">>Please enter password for user " + input[1] + " and hit enter");
							System.out.print(">");
							String containerPassword = scanner.nextLine();
							clientMap.put(input[1], next);
							currentClient = next;
	
							// Safe credentials to file.
							TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "password", input[2] + "--");
							TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "client_id", input[3].split("_")[1]);
							TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "client_secret", input[4]);
							
							TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "access_token", next.getAccessToken());
							TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "refresh_token", next.getRefreshToken());
							TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "session_id", next.getSessionId());
							TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "request_id", next.getRequestId() + "---");

							System.out.println(">>Successfully connected to client " + input[1] + ".");
						} catch (Exception e) {
							// TODO Auto-generated catch block
							System.out.println(">>Wrong credentials.");
							e.printStackTrace();
						}
					} else {
						System.out.println(">>User " + input[1] + " already connected. Try <command> <username> or type help for more information.");
					}
				} else {
					System.out.println(">>new <username> <password> <client_id> <client_secret>");
				}
			} else if(input.length >= 2 && input[0].equals("logout")) {
				if(clientMap.get(input[1]) != null) {
					clientMap.get(input[1]).logout();
					clientMap.remove(input[1]);
					System.out.println(">>Succesfull logout of user " + input[1] + ".");
				} else {
					System.out.println(">>User not found.");
				}
			} else if(input[0].equals("balance")) {
				Client user = currentClient;
				
				if(input.length >= 2) {
					user = clientMap.get(input[1]);
				}
				
				if(user != null) {
					System.out.println(user.getBalance());
				} else {
					System.out.println("User not found.");
				}
			}
		}
		scanner.close();
	}
    
}

package application;

import java.io.Console;
import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import client.Client;
import client.TokenHandling;


public class App {
	private HashMap<String, Client> clientMap;
	private Client currentClient;
	Scanner scanner  = new Scanner(System.in);
	Console console = System.console();

	/**
	 * If the user already excists and 
	 * @param containerPassword Password for the KeyStore.
	 * @param username 
	 */
	private void reconnect(String containerPassword, String username) {
		try {
			String fileName = "./" + username + "-user";
			// Get credentials from file and connect user.
			String access_token = TokenHandling.getFromPKCS12KeyStore(containerPassword, fileName, "access_token");
			String refresh_token = TokenHandling.getFromPKCS12KeyStore(containerPassword, fileName, "refresh_token").split("=")[0];
			String request_id = TokenHandling.getFromPKCS12KeyStore(containerPassword, fileName, "request_id").split("-")[0];
			String session_id = TokenHandling.getFromPKCS12KeyStore(containerPassword, fileName, "session_id");
			
			if(clientMap.get(username) == null) {
				currentClient = new Client(username, 
					access_token, refresh_token, request_id, session_id);
				clientMap.put(username, currentClient);
			}

			try {
				refresh(containerPassword);
			} catch (Exception e) {
				System.out.println(">>New authentification required.");
				connect(currentClient.getUsername(), containerPassword);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * Adds a user and save credentials to PKCS12KeyStore.
	 * @param input List of the credentials.
	 */
	private void add(String[] input) {
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

					// Safe credentials to KeyStore.
					TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "password", input[2] + "--");
					TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "client_id", input[3].split("_")[1]);
					TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "client_secret", input[4]);
					
					TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "access_token", next.getAccessToken());
					TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "refresh_token", next.getRefreshToken());
					TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "session_id", next.getSessionId());
					TokenHandling.saveToPKCS12KeyStore(containerPassword, input[1] + "-user", "request_id", next.getRequestId() + "---");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println(">>Wrong credentials.");
				}
			} else {
				System.out.println(">>User " + input[1] + " already connected. Try <command> <username> or type help for more information.");
			}
		} else {
			System.out.println(">>new <username> <password> <client_id> <client_secret>");
		}
	}

	/**
	 * Connects an already excisting user to comdirect and updates token.
	 * @param username 
	 * @param containerPassword password for PKCS12KeyStore.
	 */
	private void connect(String username, String containerPassword) {
		// Go through directory and connect matching user.
		File dir = new File("./");
		File[] dirList = dir.listFiles();

		for(File cur : dirList) {
			String[] name  = cur.getName().split("-");

			if(name.length == 2 && name[1].equals("user") && username.equals(name[0])){
				try {

					// Get credentials from file and connect user.
					String password = TokenHandling.getFromPKCS12KeyStore(containerPassword, cur.getName(), "password").split("-")[0];
					String client_id = "User_" + TokenHandling.getFromPKCS12KeyStore(containerPassword, cur.getName(), "client_id");
					String client_secret = TokenHandling.getFromPKCS12KeyStore(containerPassword, cur.getName(), "client_secret");
					
					// Connect client.
					Client next = new Client(name[0]);
					next.connect(password, client_id, client_secret);
					
					// Udpdates credentials which are changed during the tan challenge.
					TokenHandling.saveToPKCS12KeyStore(containerPassword, username + "-user", "access_token", next.getAccessToken());
					TokenHandling.saveToPKCS12KeyStore(containerPassword, username + "-user", "refresh_token", next.getRefreshToken());
					TokenHandling.saveToPKCS12KeyStore(containerPassword, username + "-user", "session_id", next.getSessionId());
					TokenHandling.saveToPKCS12KeyStore(containerPassword, username + "-user", "request_id", next.getRequestId() + "---"); 

					System.out.println(">>Successfully connected to user " + username + ".");
					clientMap.put(name[0], next);
					currentClient = next;
				} catch (Exception e) {
					System.out.println(">>Couldn't connect to user " + username + ".");
					e.printStackTrace();
				}
			}
		}
	}

	private void balance() {
		JSONArray accounts = null;
		if(currentClient != null) {
			try {
				accounts = currentClient.getBalances().getJSONArray("values");
			} catch (Exception e) {
				// Try if access token is expired.
				try {
					refresh("Land2070");
					accounts = currentClient.getBalances().getJSONArray("values");
				} catch (Exception e1) {
					System.out.println("Couldn´t request balance, nor refresh tokens. New authentification required. Try connect <user>.");
				}
			}
		} else {
			System.out.println("User not found.");
		}
		
		for(int i = 0; i<accounts.length(); ++i) {
			JSONObject balance = null;
			JSONObject avBalance = null;
			try {
				balance = accounts.getJSONObject(i).getJSONObject("balance");
				avBalance = accounts.getJSONObject(i).getJSONObject("availableCashAmount");
				System.out.println(">>Account " + i 
				+ ": Balance = " + balance.getString("value") + balance.getString("unit") 
				+ ", Available balance = " + avBalance.getString("value") + avBalance.getString("unit"));
			} catch (JSONException e) {
				break;
			}
		}
		
	}

	private void transactions() {
		JSONArray transactions = null;
		
		try {
			transactions = currentClient.getTransactions().getJSONArray("values");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(transactions);
	}

	private void logout() {
		if(currentClient != null) {
			currentClient.revoke();
			clientMap.remove(currentClient.getUsername());
			System.out.println(">>Succesfull logout of user " + currentClient.getUsername() + ".");
		} else {
			System.out.println(">>User not found.");
		}
	}

	private void refresh(String containerPassword) throws Exception{
		// Get credentials from file and connect user.
		String password = TokenHandling.getFromPKCS12KeyStore(containerPassword, currentClient.getUsername() + "-user", "password").split("-")[0];
		String client_id = "User_" + TokenHandling.getFromPKCS12KeyStore(containerPassword, currentClient.getUsername() + "-user", "client_id");
		String client_secret = TokenHandling.getFromPKCS12KeyStore(containerPassword, currentClient.getUsername() + "-user", "client_secret");
		currentClient.refresh(password, client_id, client_secret);
		
		// Save new tokens.
		TokenHandling.saveToPKCS12KeyStore(containerPassword, currentClient.getUsername() + "-user", "access_token", currentClient.getAccessToken());
		TokenHandling.saveToPKCS12KeyStore(containerPassword, currentClient.getUsername() + "-user", "refresh_token", currentClient.getRefreshToken());
	}

	public App() {
		clientMap = new HashMap<String, Client>();
		currentClient = null;
	}

	public void start() {

		while(true) {
			System.out.print(">");
			String[] input = scanner.nextLine().split(" +");

			String command = input[0];
			if(input.length == 5) {
				if(command.equals("add")) {
					add(input);
				}
			} else if(input.length == 1) {
				if(command.equals("quit")) {
					break;
				} else if(command.equals("reconnect")) {
					reconnect("Land2070", "69347337");
				} else if(command.equals("connect")) {
					connect(input[1], "Land2070");
				} else if(command.equals("balance")) {
					balance();
				} else if(command.equals("logout")) {
					logout();
				} else if(command.equals("refresh")) {
					try {
						refresh("Land2070");
					} catch (Exception e) {
						System.out.println(">>Couldn't refresh tokens. Try connect <user>.");
					}
				} else if(command.equals("transactions")) {
					transactions();
				} else {
					System.out.println(">>Command not found. Try help.");
				}
			} else {
				System.out.println(">>Command not found. Try help.");
			}
		}
		scanner.close();
	}
    
}

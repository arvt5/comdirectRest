package client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.Timestamp;
import java.util.Scanner;
import java.util.UUID;

import org.json.JSONObject;
import org.json.JSONArray;

public class Client {
	private String access_token;
	private String refresh_token;
	private String url = "https://api.comdirect.de/api";
	
	public Client() {
		
	}

	/**
	 * 
	 */
	public void logout() {
		
		try {
			HttpRequest httpRequest = HttpRequest.newBuilder()
						.uri(new URI("https://api.comdirect.de/oauth/token"))
						.headers("Content-Type", "application/x-www-form-urlencoded", "Accept", "application/json", "Authorization", "Bearer %s".formatted(this.access_token))
						.POST(BodyPublishers.ofString(""))
						.build();

			HttpClient httpClient = HttpClient.newHttpClient();
			HttpResponse<String> httpResponse =  httpClient.send(httpRequest, BodyHandlers.ofString());

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
			
			

	}


	/**
	 * @param client_id
	 * @param client_secret
	 * @param username
	 * @param password
	 * @throws Exception
	 */
	public void connect(String username, String password, String client_id, String client_secret) throws Exception {
		
		// POST https://api.comdirect.de/oauth/token
		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(new URI("https://api.comdirect.de/oauth/token"))
				.headers("Content-Type", "application/x-www-form-urlencoded", "Accept", "application/json")
				.POST(BodyPublishers.ofString(
						"client_id=%s".formatted(client_id)
						+ "&client_secret=%s".formatted(client_secret)
						+ "&grant_type=password"
						+ "&username=%s".formatted(username)
						+ "&password=%s".formatted(password)))
				.build();
		
		HttpClient httpClient = HttpClient.newHttpClient();
		
		HttpResponse<String> httpResponse =  httpClient.send(httpRequest, BodyHandlers.ofString());
		
		// Check if request has failed.  
		if(httpResponse.statusCode() != 200) 
			throw new Exception("POST https://api.comdirect.de/oauth/token returned %s".formatted(httpResponse.statusCode()));
		
		JSONObject response = new JSONObject(httpResponse.body());
		
		this.access_token = response.getString("access_token");
		this.refresh_token = response.getString("refresh_token");
		
		// Generate session_id and request_id.
		String session_id = UUID.randomUUID().toString();
		
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		
		String request_id = Long.toString(timestamp.getTime());
		request_id = request_id.substring(request_id.length() - 9);
		
		// GET URL-Präfix/session/clients/{clientId}/v1/sessions
		String authorization = "Bearer %s".formatted(access_token);			
		
		String requestInfo = "{\"clientRequestId\":{\"sessionId\":\"%s\",\"requestId\":\"%s\"}}".formatted(session_id, request_id);
		httpRequest = HttpRequest.newBuilder()
				.uri(new URI(
						"%s/session/clients/user/v1/sessions".formatted(url)
						))
				.headers(
						"Accept", "application/json", "Authorization", authorization, "x-http-request-info", requestInfo, "Content-Type", "application/json"
						)
				.build();
		
		httpResponse =  httpClient.send(httpRequest, BodyHandlers.ofString());
		
		if(httpResponse.statusCode() != 200)
			throw new Exception("GET URL-Präfix/session/clients/{clientId}/v1/sessions returned %s".formatted(httpResponse.statusCode()));
		
		JSONArray responseArray = new JSONArray(httpResponse.body());
		response = new JSONObject(responseArray.get(0).toString());
		String identifier = response.getString("identifier");
		
		// POST URL-Präfix/session/clients/{clientId}/v1/sessions/{sessionId}/validate
		httpRequest = HttpRequest.newBuilder()
				.uri(new URI(
						"%s/session/clients/%s/v1/sessions/%s/validate".formatted(url, client_id, identifier) 
						))
				.headers(
						"Accept", "application/json", "Authorization", authorization, "x-http-request-info", requestInfo, "Content-Type", "application/json"
						)
				.POST(BodyPublishers.ofString("{\r\n\"identifier\":\"%s\",\r\n\"sessionTanActive\":true,\r\n\"activated2FA\":true\r\n}".formatted(identifier)))
				.build();
		
		httpResponse =  httpClient.send(httpRequest, BodyHandlers.ofString());
		
		if (httpResponse.statusCode() != 201)
			throw new Exception("POST URL-Präfix/session/clients/{clientId}/v1/sessions/{sessionId}/validate returned %s".formatted(httpResponse.statusCode()));
		
		response = new JSONObject(httpResponse.headers().allValues("x-once-authentication-info").get(0));
		
		// Wait until user confirms tan confirmation.
		System.out.println("Please press enter after confirming tan.");
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		
		
		// PATCH URL-Präfix/session/clients/{clientId}/v1/sessions/{sessionId}
		httpRequest = HttpRequest.newBuilder()
				.uri(new URI(
						"%s/session/clients/%s/v1/sessions/%s".formatted(url, client_id, identifier) 
						))
				.method("PATCH", BodyPublishers.ofString("{\r\n\"identifier\":\"%s\",\r\n\"sessionTanActive\":true,\r\n\"activated2FA\":true\r\n}\r\n".formatted(identifier)))
				.headers(
						"Accept", "application/json", "Authorization", authorization, "x-http-request-info", requestInfo, "Content-Type", "application/json",
						"x-once-authentication-info", "{\"id\":\"%s\"}".formatted(response.get("id")))
				.build();
		
		httpResponse =  httpClient.send(httpRequest, BodyHandlers.ofString());
		
		if (httpResponse.statusCode() != 200)
			throw new Exception("PATCH URL-Präfix/session/clients/{clientId}/v1/sessions/{sessionId} returned %s".formatted(httpResponse.statusCode()));
		
		// POST https://api.comdirect.de/oauth/token
		httpRequest = HttpRequest.newBuilder()
				.uri(new URI("https://api.comdirect.de/oauth/token"))
				.headers("Accept", "application/json", "Content-Type", "application/x-www-form-urlencoded")
				.POST(BodyPublishers.ofString("client_id=%s&".formatted(client_id)
						+ "client_secret=%s&".formatted(client_secret)
						+ "grant_type=cd_secondary&"
						+ "token=%s".formatted(access_token)))
				.build();
		
		httpResponse =  httpClient.send(httpRequest, BodyHandlers.ofString());
		
		if (httpResponse.statusCode() != 200)
			throw new Exception("POST https://api.comdirect.de/oauth/token returned %s".formatted(httpResponse.statusCode()));
		
		response = new JSONObject(httpResponse.body());
		
		this.access_token = response.getString("access_token");
		this.refresh_token = response.getString("refresh_token");
		
		

		// Safe token to file.
		TokenHandling.saveToPKCS12KeyStore("Land2070", username, "access_token",this.access_token);
		TokenHandling.saveToPKCS12KeyStore("Land2070", username, "refresh_token", this.refresh_token);
		
	}


}


package hackzurich;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class FireBase {
	
	
	
	private final String USER_AGENT = "Mozilla/5.0";

	public static void main(String args[]) {
		
		try {
	
		FireBase fBase = new FireBase();
		System.out.println("\nTesting - Send Http POST request");
		fBase.sendPost();
		

		}
		catch(Exception e) 	{
			e.printStackTrace();
		}
		
	}
	
	
	

		
	public void sendPost() throws Exception {
	HttpClient httpClient = new DefaultHttpClient();

	try {
	    HttpPost request = new HttpPost("https://fcm.googleapis.com/fcm/send");
	    request.addHeader("content-type", "application/json");
	   // request.addHeader("Accept","application/json");
	    request.addHeader("Authorization","key=AIzaSyATEU5Q4_ILtSJKYA07Gb1OD156akTL9VI");
  
		    
	   // StringEntity params =new StringEntity("{ \"data\": {\"score\":\"5\" }, \"to\" : \"dKMrwsZkoq4:APA91bEspep0eOUfnvop7pqlqJtyv9HTTub0hvaxC707FBGpnIHM146dUw-5zGkNdymYWDVYldIrPdrGhOFUL__beLscEwf7HsebQ69a9-bON57eBNPfJzhmIh1XqFr4KA7TY1dL2zZ1\"}");
	    
	   
	    StringEntity params =new StringEntity("{ \"notification\": {\"body\":\"great match!\" }, \"to\" : \"dKMrwsZkoq4:APA91bEspep0eOUfnvop7pqlqJtyv9HTTub0hvaxC707FBGpnIHM146dUw-5zGkNdymYWDVYldIrPdrGhOFUL__beLscEwf7HsebQ69a9-bON57eBNPfJzhmIh1XqFr4KA7TY1dL2zZ1\"}");
	    
	    
	    request.setEntity(params);
	    System.out.println("---------------"+request.toString());
	    HttpResponse response = httpClient.execute(request);

	    // handle response here...
	}catch (Exception ex) {
	    // handle exception here
	} finally {
	    httpClient.getConnectionManager().shutdown();
	}
	
	}
	
	
	
	
}

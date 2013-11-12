package usc.linkage.jersey.IT;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;


import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import com.sun.jersey.api.client.ClientResponse;




public class WebResourceSmokeTest extends JerseyTestFrame{
	
	@Test(description="Smoke test")
	public void testIT() throws JSONException, InterruptedException, ExecutionException, IOException{
		String url = "https://dl.dropboxusercontent.com/s/nr8v39tyhadgure/json.txt?token_hash=AAEAzC_8VbQYQCl_Cy6xIJJSpYv0jl4_Dzjg8-i5MeygiA&amp;dl=1";
		AsyncHttpClient httpClient=new AsyncHttpClient();
		Request request=httpClient.prepareGet(url).build();
		Response response=httpClient.executeRequest(request).get();
		String responseBody=response.getResponseBody();
		
		ClientResponse clientResponse = null;
		clientResponse=getWebResource("/link").post(ClientResponse.class, responseBody);
		String str = clientResponse.getEntity(String.class);
		JSONObject retJson = new JSONObject(str);
		clientResponse=getWebResource("/data/"+retJson.getString("resultFile")).get(ClientResponse.class);
		System.out.println(clientResponse.getEntity(String.class));
	}
	
}
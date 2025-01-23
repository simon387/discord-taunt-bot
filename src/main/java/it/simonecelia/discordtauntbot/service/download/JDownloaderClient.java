package it.simonecelia.discordtauntbot.service.download;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.simonecelia.discordtauntbot.config.AppConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;


@ApplicationScoped
public class JDownloaderClient {

	@Inject
	AppConfig appConfig;

	private static final String BASE_URL = "https://api.jdownloader.org";

	private String deviceName;

	private String email;

	private String password;

	@PostConstruct
	public void OnStartup () {
		this.deviceName = appConfig.getJdownloaderDeviceName ();
		this.email = appConfig.getJdownloaderEmail ();
		this.password = appConfig.getJdownloaderPassword ();
	}

	public boolean addDownload_ ( String downloadLink ) {
		main ( null );
		return true;
	}

	private final String MY_JD_URL = "https://api.jdownloader.org";

	private final ObjectMapper objectMapper = new ObjectMapper ();

	private String sessionToken;

	private String regainToken;

	public void main ( String[] args ) {
		try ( CloseableHttpClient httpClient = HttpClients.createDefault () ) {
			// Login
			boolean loginSuccess = performLogin ( httpClient );
			if ( !loginSuccess ) {
				System.out.println ( "Login failed!" );
				return;
			}

			// Get devices
			String devices = getDevices ( httpClient );
			System.out.println ( "Devices response: " + devices );

			// Add download only if we have valid authentication
			if ( loginSuccess && !devices.contains ( "BAD_PARAMETERS" ) ) {
				String linkToDownload = "https://example.com/file.zip";
				boolean downloadAdded = addDownload ( httpClient, linkToDownload );
				System.out.println ( "Download added: " + downloadAdded );
			}

		} catch ( Exception e ) {
			e.printStackTrace ();
		}
	}

	private boolean performLogin ( CloseableHttpClient httpClient ) throws Exception {
		HttpPost request = new HttpPost ( MY_JD_URL + "/my/connect" );

		// Generate email hash and login secret
		String emailHash = createEmailHash ( email.toLowerCase () );
		String loginSecret = createLoginSecret ( emailHash, password );

		// Create connection request
		String jsonBody = String.format (
						"{\"email\":\"%s\",\"appkey\":\"my.jdownloader.org\",\"loginSecret\":\"%s\"}",
						emailHash, loginSecret
		);

		System.out.println ( "Attempting login with email hash: " + emailHash );

		request.setEntity ( new StringEntity ( jsonBody ) );
		request.setHeader ( "Content-Type", "application/json" );

		try ( CloseableHttpResponse response = httpClient.execute ( request ) ) {
			String result = EntityUtils.toString ( response.getEntity () );
			System.out.println ( "Login response: " + result );

			if ( result.contains ( "sessiontoken" ) ) {
				sessionToken = objectMapper.readTree ( result ).path ( "sessiontoken" ).asText ();
				regainToken = objectMapper.readTree ( result ).path ( "regaintoken" ).asText ();
				return true;
			}
			return false;
		}
	}

	private String getDevices ( CloseableHttpClient httpClient ) throws Exception {
		HttpGet request = new HttpGet ( MY_JD_URL + "/my/listdevices" );
		request.setHeader ( "Authorization", "Bearer " + sessionToken );
		request.setHeader ( "Accept", "application/json" );

		try ( CloseableHttpResponse response = httpClient.execute ( request ) ) {
			return EntityUtils.toString ( response.getEntity () );
		}
	}

	private boolean addDownload ( CloseableHttpClient httpClient, String linkToDownload ) throws Exception {
		HttpPost request = new HttpPost ( MY_JD_URL + "/v2/devices/" + deviceName + "/linkgrabber/add" );
		request.setHeader ( "Authorization", "Bearer " + sessionToken );

		String jsonBody = String.format ( "{\"links\":[\"%s\"],\"autostart\":true}", linkToDownload );
		request.setEntity ( new StringEntity ( jsonBody ) );
		request.setHeader ( "Content-Type", "application/json" );

		try ( CloseableHttpResponse response = httpClient.execute ( request ) ) {
			String result = EntityUtils.toString ( response.getEntity () );
			return !result.contains ( "error" ) && !result.contains ( "BAD_PARAMETERS" );
		}
	}

	private String createEmailHash ( String email ) throws Exception {
		MessageDigest md = MessageDigest.getInstance ( "SHA-256" );
		byte[] digest = md.digest ( email.getBytes ( StandardCharsets.UTF_8 ) );
		return Base64.getEncoder ().encodeToString ( digest );
	}

	private String createLoginSecret ( String emailHash, String password ) throws Exception {
		// Create login secret using email hash and password
		MessageDigest md = MessageDigest.getInstance ( "SHA-256" );
		String saltedPw = emailHash + password;
		byte[] digest = md.digest ( saltedPw.getBytes ( StandardCharsets.UTF_8 ) );
		return Base64.getEncoder ().encodeToString ( digest );
	}

	private String encrypt ( String data, String key ) throws Exception {
		Mac sha256_HMAC = Mac.getInstance ( "HmacSHA256" );
		SecretKeySpec secretKey = new SecretKeySpec ( key.getBytes ( StandardCharsets.UTF_8 ), "HmacSHA256" );
		sha256_HMAC.init ( secretKey );
		return Base64.getEncoder ().encodeToString ( sha256_HMAC.doFinal ( data.getBytes ( StandardCharsets.UTF_8 ) ) );
	}
}
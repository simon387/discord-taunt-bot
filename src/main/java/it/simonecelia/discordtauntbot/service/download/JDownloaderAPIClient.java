package it.simonecelia.discordtauntbot.service.download;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;


public class JDownloaderAPIClient {

	private static final String BASE_URL = "https://api.jdownloader.org";

	private static final String APP_KEY = "http://git.io/vmcsk";

	private static final ObjectMapper objectMapper = new ObjectMapper ();


	public static class ConnectionResult {

		public String sessionToken;

		public String regainToken;

		public String deviceId;
	}

	public static ConnectionResult connect ( String email, String password ) throws Exception {
		// Create login secret
		byte[] loginSecret = createSecretHash ( email, password, "server" );
		byte[] deviceSecret = createSecretHash ( email, password, "device" );

		// Generate request ID (timestamp in milliseconds)
		long requestId = Instant.now ().toEpochMilli ();

		// Prepare query parameters
		String queryParams = String.format (
						"email=%s&appkey=%s&rid=%d",
						URLEncoder.encode ( email, StandardCharsets.UTF_8 ),
						URLEncoder.encode ( APP_KEY, StandardCharsets.UTF_8 ),
						requestId
		);

		// Calculate signature
		String signature = calculateSignature ( loginSecret, "/my/connect?" + queryParams );

		// Construct full URL
		String fullUrl = BASE_URL + "/my/connect?" + queryParams + "&signature=" + signature;

		// Create HTTP client and request
		HttpClient client = HttpClient.newHttpClient ();
		HttpRequest request = HttpRequest.newBuilder ()
						.uri ( URI.create ( fullUrl ) )
						.GET ()
						.build ();

		// Send request and get response
		HttpResponse<String> response = client.send ( request, HttpResponse.BodyHandlers.ofString () );

		// Parse response
		if ( response.statusCode () == 200 ) {
			String decryptedResponse = decryptToString ( loginSecret, response.body () );
			JsonNode jsonResponse = objectMapper.readTree ( decryptedResponse );

			ConnectionResult result = new ConnectionResult ();
			result.sessionToken = jsonResponse.get ( "sessiontoken" ).asText ();
			result.regainToken = jsonResponse.get ( "regaintoken" ).asText ();
			//			result.sessionToken = response.body();

			byte[] deviceEncryptionToken = generateDeviceEncryptionToken ( deviceSecret, result.sessionToken );

			// Get device list
			String deviceListUrl = BASE_URL + "/my/listdevices?sessiontoken=" + result.sessionToken;
			HttpRequest deviceRequest = HttpRequest.newBuilder ()
							.uri ( URI.create ( deviceListUrl ) )
							.GET ()
							.build ();

			HttpResponse<String> deviceResponse = client.send ( deviceRequest, HttpResponse.BodyHandlers.ofString () );
			String decryptedDeviceResponse = decryptToString ( deviceEncryptionToken, deviceResponse.body () );
			JsonNode deviceList = objectMapper.readTree ( decryptedDeviceResponse );

			// Take the first device
			if ( deviceList.get ( "list" ).size () > 0 ) {
				result.deviceId = deviceList.get ( "list" ).get ( 0 ).get ( "id" ).asText ();
			}

			return result;
		}

		throw new Exception ( "Connection failed: " + response.body () );
	}

	public void addAndDownloadLink ( ConnectionResult connectionInfo, String downloadLink ) throws Exception {
		HttpClient client = HttpClient.newHttpClient ();

		// Prepare the add links request
		String addLinksUrl = BASE_URL + "/t_" + connectionInfo.sessionToken + "_" + connectionInfo.deviceId + "/linkgrabberv2/addLinks";

		// Construct the JSON payload for adding links
		String jsonPayload = objectMapper.createObjectNode ()
						.put ( "url", "/linkgrabberv2/addLinks" )
						.set ( "params", objectMapper.createArrayNode ()
										.add ( objectMapper.createObjectNode ()
														.put ( "links", downloadLink )
														.put ( "packageName", "MyDownloadPackage" )
														.put ( "autostart", true )
										)
						).toString ();

		// Create request
		HttpRequest addRequest = HttpRequest.newBuilder ()
						.uri ( URI.create ( addLinksUrl ) )
						.header ( "Content-Type", "application/json" )
						.POST ( HttpRequest.BodyPublishers.ofString ( jsonPayload ) )
						.build ();

		// Send add links request
		HttpResponse<String> addResponse = client.send ( addRequest, HttpResponse.BodyHandlers.ofString () );

		// Start downloads
		String startDownloadsUrl = BASE_URL + "/t_" + connectionInfo.sessionToken + "_" + connectionInfo.deviceId + "/downloadcontroller/start";

		HttpRequest startRequest = HttpRequest.newBuilder ()
						.uri ( URI.create ( startDownloadsUrl ) )
						.POST ( HttpRequest.BodyPublishers.noBody () )
						.build ();

		HttpResponse<String> startResponse = client.send ( startRequest, HttpResponse.BodyHandlers.ofString () );
	}

	private static byte[] createSecretHash ( String email, String password, String domain ) throws Exception {
		MessageDigest md = MessageDigest.getInstance ( "SHA-256" );
		md.update ( ( email.toLowerCase () + password + domain.toLowerCase () ).getBytes ( StandardCharsets.UTF_8 ) );
		return md.digest ();
	}

	private static String calculateSignature ( byte[] key, String data ) throws Exception {
		Mac sha256Hmac = Mac.getInstance ( "HmacSHA256" );
		SecretKeySpec secretKey = new SecretKeySpec ( key, "HmacSHA256" );
		sha256Hmac.init ( secretKey );
		byte[] signatureBytes = sha256Hmac.doFinal ( data.getBytes ( StandardCharsets.UTF_8 ) );
		return bytesToHex ( signatureBytes );
	}

	private static String bytesToHex ( byte[] bytes ) {
		StringBuilder result = new StringBuilder ();
		for ( byte b : bytes ) {
			result.append ( String.format ( "%02x", b ) );
		}
		return result.toString ();
	}

	private static byte[] decrypt ( byte[] secretToken, String encryptedData ) throws Exception {
		byte[] initVector = Arrays.copyOfRange ( secretToken, 0, secretToken.length / 2 );
		byte[] key = Arrays.copyOfRange ( secretToken, secretToken.length / 2, secretToken.length );

		Cipher cipher = Cipher.getInstance ( "AES/CBC/PKCS5Padding" );
		SecretKeySpec secretKeySpec = new SecretKeySpec ( key, "AES" );
		IvParameterSpec iv = new IvParameterSpec ( initVector );

		cipher.init ( Cipher.DECRYPT_MODE, secretKeySpec, iv );
		byte[] decodedBytes = Base64.getDecoder ().decode ( encryptedData );
		byte[] decryptedBytes = cipher.doFinal ( decodedBytes );

		return unpad ( decryptedBytes );
	}

	private static byte[] unpad ( byte[] input ) {
		int padLength = input[input.length - 1];
		int newLength = input.length - padLength;
		byte[] output = new byte[newLength];
		System.arraycopy ( input, 0, output, 0, newLength );
		return output;
	}

	//	private static String decryptToString(byte[] secretToken, String encryptedData) throws Exception {
	//		byte[] decryptedBytes = decrypt(secretToken, encryptedData);
	//		return new String(decryptedBytes, StandardCharsets.UTF_8);
	//	}

	private static String decryptToString ( byte[] secretToken, String encryptedData ) throws Exception {
		String cleanedData = "";
		try {
			// Clean and validate base64 data
			cleanedData = encryptedData.replaceAll ( "[^A-Za-z0-9+/=]", "" );
			byte[] decodedBytes = Base64.getDecoder ().decode ( cleanedData );

			byte[] initVector = Arrays.copyOfRange ( secretToken, 0, secretToken.length / 2 );
			byte[] key = Arrays.copyOfRange ( secretToken, secretToken.length / 2, secretToken.length );

			Cipher cipher = Cipher.getInstance ( "AES/CBC/PKCS5Padding" );
			SecretKeySpec secretKeySpec = new SecretKeySpec ( key, "AES" );
			IvParameterSpec iv = new IvParameterSpec ( initVector );

			cipher.init ( Cipher.DECRYPT_MODE, secretKeySpec, iv );
			byte[] decryptedBytes = cipher.doFinal ( decodedBytes );

			return new String ( decryptedBytes, StandardCharsets.UTF_8 );
		} catch ( Exception e ) {
			System.err.println ( "Decryption error: " + e.getMessage () );
			System.err.println ( "Encrypted data length: " + encryptedData.length () );
			System.err.println ( "Cleaned data length: " + cleanedData.length () );
			throw e;
		}
	}

	private static byte[] generateDeviceEncryptionToken ( byte[] deviceSecret, String sessionToken ) throws Exception {
		MessageDigest digest = MessageDigest.getInstance ( "SHA-256" );
		digest.update ( deviceSecret );

		// Convert hex session token to byte array correctly
		byte[] sessionTokenBytes = hexToBytes ( sessionToken );
		digest.update ( sessionTokenBytes );

		return digest.digest ();
	}

	private static byte[] hexToBytes ( String hexString ) {
		int len = hexString.length ();
		byte[] data = new byte[len / 2];
		for ( int i = 0; i < len; i += 2 ) {
			data[i / 2] = (byte) ( ( Character.digit ( hexString.charAt ( i ), 16 ) << 4 )
							+ Character.digit ( hexString.charAt ( i + 1 ), 16 ) );
		}
		return data;
	}
}
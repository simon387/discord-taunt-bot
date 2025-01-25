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


public class JDownloaderAPIClient2 {

	private static final String BASE_URL = "https://api.jdownloader.org";

	private static final String APP_KEY = "http://git.io/vmcsk";

	private static final ObjectMapper MAPPER = new ObjectMapper ();


	public static class ConnectionResult {

		public String email;

		public String sessionToken;

		public String regainToken;

		public byte[] loginSecret;

		public byte[] deviceSecret;

		public byte[] deviceEncryptionToken;

		public String deviceId;
	}

	public static ConnectionResult connect ( String email, String password ) throws Exception {
		ConnectionResult result = new ConnectionResult ();
		result.email = email;

		// Create secrets
		result.loginSecret = createSecretHash ( email, password, "server" );
		result.deviceSecret = createSecretHash ( email, password, "device" );

		// Prepare connection request
		long requestId = Instant.now ().toEpochMilli ();
		String queryParams = String.format (
						"email=%s&appkey=%s&rid=%d",
						URLEncoder.encode ( email, StandardCharsets.UTF_8 ),
						URLEncoder.encode ( APP_KEY, StandardCharsets.UTF_8 ),
						requestId
		);

		// Calculate signature
		String signature = calculateSignature ( result.loginSecret, "/my/connect?" + queryParams );
		String fullUrl = BASE_URL + "/my/connect?" + queryParams + "&signature=" + signature;

		// Send connection request
		HttpClient client = HttpClient.newHttpClient ();
		HttpRequest request = HttpRequest.newBuilder ()
						.uri ( URI.create ( fullUrl ) )
						.GET ()
						.build ();

		HttpResponse<String> response = client.send ( request, HttpResponse.BodyHandlers.ofString () );

		if ( response.statusCode () != 200 ) {
			throw new Exception ( "Connection failed: " + response.body () );
		}

		// Decrypt response
		String decryptedResponse = decryptResponse ( result.loginSecret, response.body () );
		JsonNode jsonResponse = MAPPER.readTree ( decryptedResponse );

		// Set connection details
		result.sessionToken = jsonResponse.get ( "sessiontoken" ).asText ();
		result.regainToken = jsonResponse.get ( "regaintoken" ).asText ();

		// Generate device encryption token
		result.deviceEncryptionToken = generateDeviceEncryptionToken ( result.deviceSecret, result.sessionToken );

		// Fetch device list
		result.deviceId = fetchDeviceId ( result );

		return result;
	}

	private static String fetchDeviceId ( ConnectionResult result ) throws Exception {
		HttpClient client = HttpClient.newHttpClient ();
		String deviceListUrl = BASE_URL + "/my/listdevices?sessiontoken=" + result.sessionToken;

		HttpRequest deviceRequest = HttpRequest.newBuilder ()
						.uri ( URI.create ( deviceListUrl ) )
						.header ( "Accept", "application/json" )
						.GET ()
						.build ();

		HttpResponse<String> deviceResponse = client.send ( deviceRequest, HttpResponse.BodyHandlers.ofString () );

		if ( deviceResponse.statusCode () != 200 ) {
			throw new Exception ( "Device list fetch failed: " + deviceResponse.body () );
		}

		// Decrypt with login secret (server token)
		String decryptedDeviceList = decryptResponse ( result.loginSecret, deviceResponse.body () );
		JsonNode deviceList = MAPPER.readTree ( decryptedDeviceList );

		if ( deviceList.has ( "list" ) && deviceList.get ( "list" ).size () > 0 ) {
			return deviceList.get ( "list" ).get ( 0 ).get ( "id" ).asText ();
		}

		throw new Exception ( "No devices found" );
	}

	private static byte[] createSecretHash ( String email, String password, String domain ) throws Exception {
		MessageDigest md = MessageDigest.getInstance ( "SHA-256" );
		md.update ( ( email.toLowerCase () + password + domain.toLowerCase () ).getBytes ( StandardCharsets.UTF_8 ) );
		return md.digest ();
	}

	private static String calculateSignature ( byte[] key, String data ) throws Exception {
		Mac hmac = Mac.getInstance ( "HmacSHA256" );
		hmac.init ( new SecretKeySpec ( key, "HmacSHA256" ) );
		byte[] signatureBytes = hmac.doFinal ( data.getBytes ( StandardCharsets.UTF_8 ) );
		return bytesToHex ( signatureBytes );
	}

	private static String bytesToHex ( byte[] bytes ) {
		StringBuilder hex = new StringBuilder ();
		for ( byte b : bytes ) {
			hex.append ( String.format ( "%02x", b ) );
		}
		return hex.toString ();
	}

	private static String decryptResponse ( byte[] secretToken, String encryptedData ) throws Exception {
		// Prepare decryption parameters
		byte[] initVector = Arrays.copyOfRange ( secretToken, 0, secretToken.length / 2 );
		byte[] key = Arrays.copyOfRange ( secretToken, secretToken.length / 2, secretToken.length );

		// Base64 decode
		byte[] decodedBytes = Base64.getDecoder ().decode ( encryptedData.trim () );

		// Decrypt
		Cipher cipher = Cipher.getInstance ( "AES/CBC/PKCS5Padding" );
		SecretKeySpec secretKeySpec = new SecretKeySpec ( key, "AES" );
		IvParameterSpec iv = new IvParameterSpec ( initVector );

		cipher.init ( Cipher.DECRYPT_MODE, secretKeySpec, iv );
		byte[] decryptedBytes = cipher.doFinal ( decodedBytes );

		return new String ( decryptedBytes, StandardCharsets.UTF_8 );
	}

	private static byte[] generateDeviceEncryptionToken ( byte[] deviceSecret, String sessionToken ) throws Exception {
		MessageDigest digest = MessageDigest.getInstance ( "SHA-256" );
		digest.update ( deviceSecret );
		digest.update ( hexToBytes ( sessionToken ) );
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
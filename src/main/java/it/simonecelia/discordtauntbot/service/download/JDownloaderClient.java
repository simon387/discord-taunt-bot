package it.simonecelia.discordtauntbot.service.download;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
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

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


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
		try {

//			JDownloaderAPIClient client = new JDownloaderAPIClient();
//			JDownloaderAPIClient.ConnectionResult connectionInfo = JDownloaderAPIClient.connect(email, password);
//			client.addAndDownloadLink(connectionInfo, downloadLink);
			JDownloaderAPIClient2.ConnectionResult result = JDownloaderAPIClient2.connect(email, password);
			System.out.println("Device ID: " + result.deviceId);

		} catch ( Exception e ) {
			throw new RuntimeException ( e );
		}
		return true;
	}

	private byte[] loginSecret;
	private byte[] deviceSecret;
	private String sessionToken;
	private String regainToken;
	private byte[] serverEncryptionToken;
	private byte[] deviceEncryptionToken;
	private long requestId;
	private void initSecrets(String email, String password) throws Exception {
		loginSecret = createSecretHash(email, password, "server");
		deviceSecret = createSecretHash(email, password, "device");
	}

	private byte[] createSecretHash(String email, String password, String domain) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update((email.toLowerCase() + password + domain.toLowerCase()).getBytes(StandardCharsets.UTF_8));
		return md.digest();
	}

	private void updateRequestId() {
		requestId = Instant.now().toEpochMilli();
	}

	private String calculateSignature(byte[] key, String data) throws Exception {
		Mac sha256Hmac = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
		sha256Hmac.init(secretKey);
		byte[] signatureBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return bytesToHex(signatureBytes);
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte b : bytes) {
			result.append(String.format("%02x", b));
		}
		return result.toString();
	}

	private byte[] updateEncryptionToken(byte[] oldToken, String sessionToken) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(oldToken);
		md.update(hexToBytes(sessionToken));
		return md.digest();
	}

	private static byte[] hexToBytes(String hexString) {
		int len = hexString.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
							+ Character.digit(hexString.charAt(i+1), 16));
		}
		return data;
	}

	private byte[] encrypt(byte[] secretToken, String data) throws Exception {
		byte[] iv = Arrays.copyOfRange(secretToken, 0, secretToken.length / 2);
		byte[] key = Arrays.copyOfRange(secretToken, secretToken.length / 2, secretToken.length);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

		byte[] padded = pad(data.getBytes(StandardCharsets.UTF_8));
		byte[] encrypted = cipher.doFinal(padded);
		return Base64.getEncoder().encode(encrypted);
	}

	private byte[] decrypt(byte[] secretToken, String encryptedData) throws Exception {
		byte[] iv = Arrays.copyOfRange(secretToken, 0, secretToken.length / 2);
		byte[] key = Arrays.copyOfRange(secretToken, secretToken.length / 2, secretToken.length);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

		byte[] decoded = Base64.getDecoder().decode(encryptedData);
		byte[] decrypted = cipher.doFinal(decoded);
		return unpad(decrypted);
	}

	private byte[] pad(byte[] input) {
		int blockSize = 16;
		int padLength = blockSize - (input.length % blockSize);
		byte[] padded = new byte[input.length + padLength];
		System.arraycopy(input, 0, padded, 0, input.length);
		for (int i = input.length; i < padded.length; i++) {
			padded[i] = (byte) padLength;
		}
		return padded;
	}

	private byte[] unpad(byte[] input) {
		int padLength = input[input.length - 1];
		byte[] unpadded = new byte[input.length - padLength];
		System.arraycopy(input, 0, unpadded, 0, unpadded.length);
		return unpadded;
	} private static final String APP_KEY = "http://git.io/vmcsk";

	public boolean connect(String email, String password) throws Exception {
		updateRequestId();
		initSecrets(email, password);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			String queryParams = String.format("email=%s&appkey=%s",
							URLEncoder.encode(email, StandardCharsets.UTF_8.toString()),
							URLEncoder.encode(APP_KEY, StandardCharsets.UTF_8.toString()));

			HttpGet request = new HttpGet(BASE_URL + "/my/connect?" + queryParams +
							"&signature=" + calculateSignature(loginSecret, queryParams) +
							"&rid=" + requestId);

			try (CloseableHttpResponse response = httpClient.execute(request)) {
				String responseBody = EntityUtils.toString(response.getEntity());
				JsonNode jsonResponse = MAPPER.readTree(responseBody);

				sessionToken = jsonResponse.get("sessiontoken").asText();
				regainToken = jsonResponse.get("regaintoken").asText();

				serverEncryptionToken = updateEncryptionToken(loginSecret, sessionToken);
				deviceEncryptionToken = updateEncryptionToken(deviceSecret, sessionToken);

				return true;
			}
		}
	}    private static final ObjectMapper MAPPER = new ObjectMapper();

}
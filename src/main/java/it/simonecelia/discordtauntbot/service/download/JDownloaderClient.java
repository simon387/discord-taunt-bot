package it.simonecelia.discordtauntbot.service.download;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@ApplicationScoped
public class JDownloaderClient {

	private static final String BASE_URL = "https://api.jdownloader.org";

	private final String deviceName;

	private final String authToken;

	public JDownloaderClient ( String deviceName, String authToken ) {
		this.deviceName = deviceName;
		this.authToken = authToken;
	}

	public boolean addDownload ( String downloadLink ) {
		try {
			String payload = String.format (
							"{\"deviceName\":\"%s\",\"url\":\"%s\",\"authtoken\":\"%s\"}",
							deviceName,
							downloadLink,
							authToken
			);

			HttpRequest request = HttpRequest.newBuilder ()
							.uri ( URI.create ( BASE_URL + "/t_linkgrabberv2/addLinks" ) )
							.header ( "Content-Type", "application/json" )
							.POST ( HttpRequest.BodyPublishers.ofString ( payload ) )
							.build ();

			HttpClient client = HttpClient.newHttpClient ();
			HttpResponse<String> response = client.send ( request, HttpResponse.BodyHandlers.ofString () );

			// Controlla il codice di risposta
			return response.statusCode () == 200;

		} catch ( Exception e ) {
			e.printStackTrace ();
			return false;
		}
	}
}
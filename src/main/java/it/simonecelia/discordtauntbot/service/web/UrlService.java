package it.simonecelia.discordtauntbot.service.web;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.URISyntaxException;


@ApplicationScoped
public class UrlService {

	public boolean isValidUrl ( String url ) {
		if ( url == null || url.isEmpty () ) {
			return false;
		}
		try {
			URI uri = new URI ( url );
			return uri.getScheme () != null && ( uri.getScheme ().equals ( "http" ) || uri.getScheme ().equals ( "https" ) );
		} catch ( URISyntaxException e ) {
			return false;
		}
	}
}

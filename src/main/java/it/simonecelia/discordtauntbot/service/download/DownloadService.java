package it.simonecelia.discordtauntbot.service.download;

import io.quarkus.logging.Log;
import it.simonecelia.discordtauntbot.service.web.UrlService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@ApplicationScoped
public class DownloadService {

	@Inject
	UrlService urlService;

	@Inject
	JDownloader2Service jDownloader2Service;

	public void attemptDownload ( String content ) {
		Log.infof ( "Attempting to download from %s", content );
		var url = content.trim ();
		if ( urlService.isValidUrl ( url ) ) {
			Log.info ( "URL is valid" );
			jDownloader2Service.downloadFromUrl ( url );
		} else {
			Log.info ( "URL is invalid" );
		}
	}
}

package it.simonecelia.discordtauntbot.service.download;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@ApplicationScoped
public class JDownloader2Service {

	@Inject
	JDownloaderClient jDownloaderClient;

	public void downloadFromUrl ( String url ) {
		jDownloaderClient.addDownload ( url ); //TODO
	}
}

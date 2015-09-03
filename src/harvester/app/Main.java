package harvester.app;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import harvester.app.harvesters.DefaultHarvester;
import harvester.app.harvesters.GoogleHarvester;
import harvester.app.harvesters.Harvester;
import harvester.app.harvesters._4Chan;

public class Main {

	private static final String LOG4J_XML = "log4j.xml";

	private static final Logger logger = Logger.getLogger(Main.class);

	private static final String GOOGLE = "google.pl";

	private static final String _4CHAN = "boards.4chan.org";

	private static String getDomainName(String url) {
		URI uri;
		String domain;
		try {
			uri = new URI(url);
			domain = uri.getHost();
		} catch (URISyntaxException e) {
			logger.error("Host not found");
			return "";
		}
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	public static void main(String... args) {
		DOMConfigurator.configure(LOG4J_XML);
		if (args.length == 0) {
			logger.error("No arguments");
			return;
		}
		String domain = getDomainName(args[0]);
		if ("".equals(domain)) {
			logger.fatal("Invalid URL");
			return;
		}
		String address = args[0];
		String pathToSave = args[1];
		Harvester harvester = null;
		switch (domain) {
		case GOOGLE:
			harvester = new GoogleHarvester(address);
			logger.info("Using Google Image harvester");
			break;
		case _4CHAN:
			harvester = new _4Chan(address);
			logger.info("Using 4Chan Image harvester");
			break;
		default:
			harvester = new DefaultHarvester(address);
			logger.info("Using default harvester");
			break;
		}
		logger.info("Starting harvesting");
		harvester.harvest(pathToSave, 30);
		harvester.finish();
		logger.info("Harvesting completed");
	}
}

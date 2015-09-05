package harvester.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.management.InvalidAttributeValueException;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import harvester.app.harvesters.DefaultHarvester;
import harvester.app.harvesters.FaNotificationHarvester;
import harvester.app.harvesters.GoogleHarvester;
import harvester.app.harvesters.Harvester;
import harvester.app.harvesters._4ChanHarvester;

public class Main {

	private static final String LOG4J_XML = "log4j.xml";

	private static final Logger logger = Logger.getLogger(Main.class);

	private static final String GOOGLE = "google.pl";

	private static final String _4CHAN = "boards.4chan.org";

	private static final String FA = "furaffinity.net";

	private static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();

		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	public static void main(String... args) {
		DOMConfigurator.configure(LOG4J_XML);
		Map<Argument, String> argumentMap = null;
		try {
			argumentMap = getArgs(args);
		} catch (InvalidAttributeValueException e) {
			displayHelp();
			return;
		}
		String address = argumentMap.get(Argument.URL);
		String domain;
		try {
			domain = getDomainName(address);
		} catch (URISyntaxException e) {
			logger.error("Host not found");
			displayHelp();
			return;
		}

		String pathToSave = argumentMap.get(Argument.PATH);
		Harvester harvester = null;

		switch (domain) {
		case GOOGLE:
			harvester = new GoogleHarvester(address);
			logger.info("Using Google Image harvester");
			break;
		case _4CHAN:
			harvester = new _4ChanHarvester(address);
			logger.info("Using 4Chan Image harvester");
			break;
		case FA:
			harvester = new FaNotificationHarvester(address, argumentMap.get(Argument.LOGIN), argumentMap.get(Argument.PASSWORD));
			logger.info("Using FA Image harvester");
			break;
		default:
			harvester = new DefaultHarvester(address);
			logger.info("Using default harvester");
			break;
		}

		logger.info("Starting harvesting");
		harvester.harvest(pathToSave, argumentMap);
		harvester.finish();
		logger.info("Harvesting completed");
	}

	private static Map<Argument, String> getArgs(String... args) throws InvalidAttributeValueException {
		if (args.length == 0) {
			logger.error("No arguments");
			throw new InvalidAttributeValueException();
		}

		Map<Argument, String> map = transformArgs(args);

		return map;
	}

	private static void displayHelp() {
		String message = MessageFormat.format(
				"Help:\nChoose site by {0} parameter and path to save by {1}. If authentication is required provide login by {2} an password by {3}",
				Argument.URL.getArg(), Argument.PATH.getArg(), Argument.LOGIN.getArg(), Argument.PASSWORD.getArg());
		logger.info(message);
	}

	private static Map<Argument, String> transformArgs(String... args) {
		Map<Argument, String> map = new HashMap<>();
		boolean lastArgumentName = false;
		Argument arg = null;
		Argument lastArg = null;
		for (String i : args) {
			arg = getArgument(i);
			if (arg != null) {
				lastArg = arg;
				if (lastArgumentName) {
					continue;
				} else {
					lastArgumentName = true;
				}
			} else {
				map.put(lastArg, i);
			}
		}
		return map;
	}

	private static Argument getArgument(String val) {
		Argument arg = null;
		try {
			arg = Argument.get(val);
		} catch (IllegalArgumentException e) {
			logger.error("Invalid argument name: " + val);
		}
		return arg;
	}
}

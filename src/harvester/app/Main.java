package harvester.app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.InvalidAttributeValueException;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import harvester.app.harvesters.Harvester;
import harvester.app.harvesters.SteamMarketHarvester;

public class Main {

	private static final String LOG4J_XML = "log4j.xml";

	private static final Logger logger = Logger.getLogger(Main.class);

	private static final String ITEMS_FILE = "items.properties";

	private static final String SETTINGS_FILE = "settings.properties";

	public static void main(String... args) throws Exception {
		DOMConfigurator.configure(LOG4J_XML);
		Map<Argument, String> argumentMap = null;
		try {
			argumentMap = getArgs(args);
		} catch (InvalidAttributeValueException e) {
			displayHelp();
			return;
		}
		Properties settings = loadProps(SETTINGS_FILE);
		applySettings(settings, argumentMap);
		Properties itemsProperties = loadProps(ITEMS_FILE);
		transformItem(argumentMap, itemsProperties);
		Harvester harvester = new SteamMarketHarvester(argumentMap);

		logger.info("Using Steam market harvester");
		try {
			if (argumentMap.containsKey(Argument.LOGIN)) {
				harvester.login();
			}
			if (argumentMap.containsKey(Argument.BUY)) {
				harvester.buy(argumentMap);
			} else if (argumentMap.containsKey(Argument.ITEM)) {
				logger.info("Starting harvesting");
				harvester.harvest(argumentMap);
			}
		} finally {
			harvester.finish();
			logger.info("Completed");
		}
	}

	private static void transformItem(Map<Argument, String> argumentMap, Properties itemsProperties) {
		String key = argumentMap.get(Argument.ITEM);
		if (key == null) {
			return;
		}
		String item = itemsProperties.getProperty(key);
		if (item != null) {
			argumentMap.put(Argument.ITEM, item);
		}
	}

	private static void applySettings(Properties settings, Map<Argument, String> argumentMap) {
		settings.forEach((key, value) -> extracted(argumentMap, key, value));
	}

	private static String extracted(Map<Argument, String> argumentMap, Object key, Object value) {
		return argumentMap.put(Argument.get(key.toString()), value.toString());
	}

	private static Properties loadProps(String file) throws IOException, FileNotFoundException {
		Properties properties = new Properties();
		try (InputStream input = new FileInputStream(file)) {
			properties.load(input);
		}
		return properties;
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
		logger.info("trololo");
	}

	private static Map<Argument, String> transformArgs(String... args) {
		Map<Argument, String> map = new HashMap<>();
		boolean lastArgumentName = false;
		Argument arg = null;
		Argument lastArg = null;
		for (String i : args) {
			arg = getArgument(i.substring(1));
			if (arg != null) {
				lastArg = arg;
				if (lastArgumentName) {
					continue;
				} else {
					if (arg.isSingle()) {
						map.put(lastArg, i);
					} else {
						lastArgumentName = true;
					}
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

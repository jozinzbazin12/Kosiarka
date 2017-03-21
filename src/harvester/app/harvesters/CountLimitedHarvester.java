package harvester.app.harvesters;

import java.util.Map;

import harvester.app.Argument;

public abstract class CountLimitedHarvester extends Harvester {

	protected int limit = 3;
	protected int pos = 0;
	protected int wait = 1000;
	protected double price = Double.MAX_VALUE;

	public CountLimitedHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap);
	}

	protected void setLimit(String value) {
		try {
			limit = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			logger.error("Error parsing limit value, harvesting all");
		}
		logger.info("Items count limited to " + limit);
	}

	protected void setWait(String string) {
		if (string != null) {
			this.wait = Integer.parseInt(string);
		}
		logger.info("Wait time set to " + wait);
	}

	protected void setPriceLimit(String string) {
		if (string != null) {
			if (!logged) {
				logger.error("Max price can be used only when logged in, skipping.");
				return;
			}
			this.price = parsePrice(string);
		}
		logger.info("Max price set to " + price);
	}

	protected double parsePrice(String string) {
		string = string.replace(",", ".");
		return Double.parseDouble(string.substring(0, string.length() - 2));
	}

	@Override
	protected boolean stopWhen() {
		return pos > limit;
	}
}

package harvester.app.harvesters;

import java.util.Map;

import harvester.app.Argument;

public abstract class CountLimitedHarvester extends Harvester {

	protected int limit = Integer.MAX_VALUE;
	protected int pos = 0;
	protected int wait = 1000;

	public CountLimitedHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap);
	}

	protected void setLimit(String value) {
		try {
			limit = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			logger.error("Error parsing limit value, harvesting all images.");
		}
		logger.info("Images count limited to " + limit);
	}

	protected void setWait(String string) {
		if (string != null) {
			this.wait = Integer.parseInt(string);
		}
	}

	@Override
	protected boolean stopWhen() {
		return pos > limit;
	}
}

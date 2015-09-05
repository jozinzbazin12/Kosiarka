package harvester.app.harvesters;

public abstract class CountLimitedHarvester extends Harvester {

	protected int limit = Integer.MAX_VALUE;
	protected int pos = 0;

	public CountLimitedHarvester(String url) {
		super(url);
	}

	public CountLimitedHarvester(String url, String login, String password) {
		super(url, login, password);
	}

	protected void setLimit(String value) {
		try {
			limit = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			logger.error("Error parsing limit value, harvesting all images.");
		}
		logger.info("Images count limited to " + limit);
	}

	@Override
	protected boolean stopWhen() {
		return pos > limit;
	}
}

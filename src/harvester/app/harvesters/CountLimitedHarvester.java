package harvester.app.harvesters;

import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import harvester.app.Argument;

public abstract class CountLimitedHarvester extends Harvester {

	protected int limit = 3;
	protected int pos = 0;
	protected int wait = 1000;
	protected double price = Double.MAX_VALUE;

	public CountLimitedHarvester(Map<Argument, String> argumentMap, String url) {
		super(argumentMap, url);
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
		try {
			return Double.parseDouble(string.substring(0, string.length() - 2));
		} catch (IllegalArgumentException e) {
			logger.error("Error while parsing price " + string, e);
		}
		return 0;
	}

	@Override
	protected boolean stopWhen() {
		return pos > limit;
	}

	private void doScreens(List<Item> items) throws InterruptedException {
		logger.debug(items);
		logger.info(String.format("Found %d items", items.size()));
		driver.get("https://metjm.net/csgo/");
		for (Item i : items) {
			driver.findElement(By.xpath("//div[@class='inspectLinkContainer']/input")).sendKeys(i.getLink());
			Thread.sleep(100);
			driver.findElement(By.xpath("//div[@class='inspectLinkContainer']/div")).click();
		}
	}

	protected void collectScreens(String pathToSave, List<Item> items) throws InterruptedException {
		doScreens(items);
		wait(items);

		List<WebElement> elements = driver.findElements(By.xpath("//div[@class='openImageButton' and @style]"));
		int id = elements.size() - 1;
		long time = System.currentTimeMillis();
		for (Item i : items) {
			try {
				WebElement el = elements.get(id--);
				String attribute = el.getAttribute(STYLE);
				int urlpos = attribute.indexOf("url(\"");
				int endurlpos = attribute.indexOf("\");");
				String url = attribute.substring(urlpos + 5, endurlpos);
				url = url.replace("_t.jpg", ".jpg");
				createSaveThread(url, pathToSave + "/" + time, i.getId() + ".jpg");
			} catch (Exception e) {
				logger.error("Error while downloading " + i.getId(), e);
			}
		}
	}

	private void wait(List<Item> items) {
		String waitMsg = String.format("Waitig %dms for metjm...", wait);

		while (driver.findElements(By.xpath("//div[@class='openImageButton' and @style]")).size() != items.size()) {
			try {
				logger.info(waitMsg);
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

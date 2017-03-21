package harvester.app.harvesters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import harvester.app.Argument;

public class SteamMarketHarvester extends CountLimitedHarvester {

	private static final String SESSION_DAT = "session.dat";

	public SteamMarketHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap);
	}

	@SuppressWarnings("deprecation")
	private void collectItemsOnPage(Set<String> items, Set<String> ids) {
		List<WebElement> elements = driver.findElements(By.className("market_listing_row"));
		for (WebElement i : elements) {
			if (stopWhen()) {
				return;
			}
			JavascriptExecutor js = (JavascriptExecutor) driver;
			try {
				WebElement findElement = i.findElement(By.xpath(".//a[@class='market_actionmenu_button']"));
				js.executeScript("arguments[0].click();", findElement);
				WebElement elem = driver.findElement(By.xpath("//div[@id='market_action_popup_itemactions']/a"));
				String href = elem.getAttribute(HREF);
				WebElement idElem = i.findElement(By.xpath(".//div[@class='market_listing_buy_button']/a"));

				String attribute = idElem.getAttribute(HREF);
				attribute = URLDecoder.decode(attribute);
				int pos = attribute.indexOf("listing', '");
				int endpos = attribute.indexOf("'", pos + 11);
				String id = attribute.substring(pos + 11, endpos);
				WebElement price = i
						.findElement(By.xpath(".//span[@class='market_listing_price market_listing_price_with_fee']"));

				items.add(href);
				String priceStr = price.getText();
				if (this.price < parsePrice(priceStr)) {
					logger.info("Max price reached");
					this.pos = Integer.MAX_VALUE / 2;
				}
				ids.add(priceStr + "-" + id);
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	private WebElement searchItem(String itemId) {
		try {
			this.pos += 10;
			return driver.findElement(
					By.xpath(String.format(".//div[@class='market_listing_buy_button']/a[contains(@href,'%s')]", itemId)));
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}

	@Override
	public void harvest(Map<Argument, String> args) throws InterruptedException {
		setLimit(args.get(Argument.LIMIT));
		setWait(args.get(Argument.WAIT));
		setPriceLimit(args.get(Argument.MAX_PRICE));
		String pathToSave = args.get(Argument.PATH);
		Set<String> items = new HashSet<>();
		Set<String> ids = new HashSet<>();
		gotoPage(args.get(Argument.START));

		while (!stopWhen()) {
			collectItemsOnPage(items, ids);
			this.pos++;
			Thread.sleep(wait);
			nextPage();
		}
		logger.debug(items);
		logger.info(String.format("Found %d items", ids.size()));
		driver.get("https://metjm.net/csgo/");
		for (String url : items) {
			driver.findElement(By.xpath("//div[@class='inspectLinkContainer']/input")).sendKeys(url);
			driver.findElement(By.xpath("//div[@class='inspectLinkContainer']/div")).click();
		}

		String waitMsg = String.format("Waitig %dms for metjm...", wait);
		while (driver.findElements(By.xpath("//div[@class='openImageButton' and @style]")).size() != items.size()) {
			try {
				logger.info(waitMsg);
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		collectScreens(pathToSave, new ArrayList<>(ids));
	}

	private void collectScreens(String pathToSave, List<String> ids) {
		driver.navigate().refresh();
		List<WebElement> findElements = driver.findElements(By.xpath("//div[@class='openImageButton' and @style]"));
		int id = ids.size();
		long time = System.currentTimeMillis();
		for (WebElement i : findElements) {
			try {
				--id;
				if (id < 0) {
					break;
				}
				processScreen(pathToSave, ids, id, time, i);
			} catch (Exception e) {
				logger.error("Error while downloading " + ids.get(id));
			}
		}
	}

	private void processScreen(String pathToSave, List<String> ids, int id, long time, WebElement i) {
		String attribute = i.getAttribute(STYLE);
		int urlpos = attribute.indexOf("url(\"");
		int endurlpos = attribute.indexOf("\");");
		String url = attribute.substring(urlpos + 5, endurlpos);
		url = url.replace("_t.jpg", ".jpg");
		createSaveThread(url, pathToSave + "/" + time, ids.get(id) + ".jpg");
	}

	private void gotoPage(String start) throws InterruptedException {
		if (start != null) {
			int startPage = Integer.parseInt(start) - 1;
			for (int i = 0; i < startPage; i++) {
				nextPage();
				this.pos++;
				Thread.sleep(wait);
			}
		}
	}

	private void nextPage() {
		driver.findElement(
				By.xpath("//span[@id='searchResults_links']/span[@class='market_paging_pagelink active']/following::*")).click();
	}

	@Override
	public void login() throws FileNotFoundException, IOException {
		driver.get("https://steamcommunity.com/login/home/?goto=market%2F");
		logger.info("Plese login now and press enter when you finish");
		System.in.read();
		Set<Cookie> cookies = driver.manage().getCookies();
		try (FileOutputStream fileOut = new FileOutputStream(SESSION_DAT);
				ObjectOutputStream os = new ObjectOutputStream(fileOut)) {
			os.writeObject(cookies);
		}
		logger.info("Session saved");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void restoreSession() throws FileNotFoundException, IOException, ClassNotFoundException {
		logger.info("Loading session...");
		Set<Cookie> cookies;
		try (FileInputStream fileIn = new FileInputStream(SESSION_DAT); ObjectInputStream is = new ObjectInputStream(fileIn)) {
			cookies = (Set<Cookie>) is.readObject();
		}
		for (Cookie c : cookies) {
			try {
				driver.manage().addCookie(c);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		driver.navigate().refresh();
		logger.info("Session loaded");
	}

	@Override
	public void buy(Map<Argument, String> args) throws InterruptedException {
		if (!logged) {
			logger.error("You must be logged in to buy!");
			return;
		}
		setLimit(args.get(Argument.LIMIT));
		setWait(args.get(Argument.WAIT));
		String id = args.get(Argument.BUY);
		WebElement searchItem = null;
		while (!stopWhen()) {
			searchItem = searchItem(id);
			if (searchItem != null) {
				break;
			}
			Thread.sleep(wait);
			nextPage();
		}
		if (searchItem == null) {
			logger.error("Could not find specified item!");
			return;
		}
		searchItem.click();
		driver.findElement(By.id("market_buynow_dialog_accept_ssa")).click();
		driver.findElement(By.id("market_buynow_dialog_purchase")).click();
		for (int i = 0; i < 10; i++) {
			try {
				driver.findElement(By.id("market_buynow_dialog_close"));
				logger.info("Item bought!");
				break;
			} catch (Exception e) {
				logger.info("Waiting for buy response...");
				Thread.sleep(wait);
			}

		}

		Thread.sleep(wait);
	}

}

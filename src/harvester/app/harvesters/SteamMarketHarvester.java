package harvester.app.harvesters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
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

	@Override
	public void harvest(Map<Argument, String> args) throws InterruptedException {
		setLimit(args.get(Argument.LIMIT));
		setWait(args.get(Argument.WAIT));
		String pathToSave = args.get(Argument.PATH);
		List<String> items = new ArrayList<>();
		List<String> ids = new ArrayList<>();

		while (!stopWhen()) {
			collectItemsOnPage(items, ids);
			Thread.sleep(wait);
			driver.findElement(
					By.xpath("//span[@id='searchResults_links']/span[@class='market_paging_pagelink active']/following::*"))
					.click();
		}
		logger.info(items);
		driver.get("https://metjm.net/csgo/");
		for (String url : items) {
			driver.findElement(By.xpath("//div[@class='inspectLinkContainer']/input")).sendKeys(url);
			driver.findElement(By.xpath("//div[@class='inspectLinkContainer']/div")).click();
		}
		while (driver.findElements(By.className("openImageButton")).size() - 1 != items.size()) {
			try {
				logger.info("Waitig 1s for metjm...");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		List<WebElement> findElements = driver.findElements(By.className("openImageButton"));
		int id = ids.size() - 1;
		long time = System.currentTimeMillis();
		for (WebElement i : findElements) {
			String attribute = i.getAttribute("style");
			if (attribute == null || attribute.equals("")) {
				break;
			}
			int urlpos = attribute.indexOf("url(\"");
			int endurlpos = attribute.indexOf("\");");
			String url = attribute.substring(urlpos + 5, endurlpos);
			url = url.replace("_t.jpg", ".jpg");
			createSaveThread(url, pathToSave + "/" + time, ids.get(id--) + ".jpg");
		}
	}

	@SuppressWarnings("deprecation")
	private void collectItemsOnPage(List<String> items, List<String> ids) {
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
				items.add(href);
				ids.add(id);
				this.pos++;
			} catch (Exception e) {
				logger.error(e);
			}
		}
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
			driver.manage().addCookie(c);
		}
		logger.info("Session loaded");
	}

}
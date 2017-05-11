package harvester.app.harvesters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import harvester.app.Argument;

public class SteamMarketHarvester extends MetjmHarvester {

	public SteamMarketHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap, argumentMap.get(Argument.ITEM));
		logger.info("Using Steam market harvester");
	}

	@SuppressWarnings("deprecation")
	private void collectItemsOnPage(List<Item> items) {
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

				String priceStr = price.getText();
				if (this.price < parsePrice(priceStr)) {
					logger.info("Max price reached");
					this.pos = Integer.MAX_VALUE / 2;
				}
				Item item = new Item(priceStr + "-" + id, href);
				items.add(item);
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	private WebElement searchItem(String itemId) {
		this.pos++;
		try {
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
		List<Item> items = new ArrayList<>();
		gotoPage(args.get(Argument.START));

		while (!stopWhen()) {
			collectItemsOnPage(items);
			this.pos++;
			Thread.sleep(wait);
			nextPage();
		}
		items = items.stream().distinct().collect(Collectors.toList());
		collectScreens(pathToSave, items, "market");
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
		steamLogin();
	}

	@Override
	public void restoreSession() throws FileNotFoundException, IOException, ClassNotFoundException {
		steamRestore();
	}

	@Override
	public void buy(Map<Argument, String> args) throws InterruptedException {
		if (!logged) {
			logger.error("You must be logged in to buy!");
			return;
		}
		setLimit(args.get(Argument.LIMIT));
		setWait(args.get(Argument.WAIT));
		gotoPage(args.get(Argument.START));
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

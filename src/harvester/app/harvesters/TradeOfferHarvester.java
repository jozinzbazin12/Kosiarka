package harvester.app.harvesters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import harvester.app.Argument;

public class TradeOfferHarvester extends CountLimitedHarvester {

	public TradeOfferHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap, argumentMap.get(Argument.LINK));
		logger.info("Using Steam market harvester");
	}

	private void collectItemsOnPage(List<Item> items) {
		List<WebElement> elements = driver.findElements(By.xpath("//a[@class='inventory_item_link']"));
		elements = elements.parallelStream().filter(el -> el.isDisplayed()).collect(Collectors.toList());
		for (WebElement i : elements) {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			try {
				WebElement findElement = i.findElement(By.xpath("./../..//a[@class='slot_actionmenu_button']"));
				js.executeScript("arguments[0].click();", findElement);
				WebElement elem = driver.findElement(By.xpath("//div[@id='trade_action_popup_itemactions']/a[1]"));
				String href = elem.getAttribute(HREF);

				Item item = new Item(String.valueOf(System.currentTimeMillis()), href);
				items.add(item);
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	@Override
	public void harvest(Map<Argument, String> args) throws InterruptedException {
		if (!logged) {
			logger.error("Login first!");
			return;
		}
		setLimit(args.get(Argument.LIMIT));
		setWait(args.get(Argument.WAIT));
		String pathToSave = args.get(Argument.PATH);
		String item = args.get(Argument.ITEM);
		List<Item> items = new ArrayList<>();
		waitForPageLoad();
		new FluentWait<>(driver).withTimeout(10, TimeUnit.SECONDS).pollingEvery(200, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class).until(ExpectedConditions.presenceOfElementLocated(By.id("appselect")));

		waitForInv();
		Thread.sleep(wait);
		waitAndClick(By.id("inventory_select_their_inventory"));
		waitForInv();
		waitAndClick(By.id("appselect"));
		waitForInv();
		waitAndClick(By.id("appselect_option_them_730_2"));
		driver.findElement(By.id("filter_control")).sendKeys(item);

		waitForInv();

		while (!stopWhen()) {
			collectItemsOnPage(items);
			if (!nextPage()) {
				break;
			}
		}
		items = items.stream().distinct().collect(Collectors.toList());
		String path = collectScreens(pathToSave, items, "offer");
		File f = new File(path + File.separator + "link.txt");
		try {
			f.createNewFile();
		} catch (IOException e) {
			logger.error(e);
		}
		try (FileWriter writer = new FileWriter(f)) {
			writer.write(args.get(Argument.LINK));
		} catch (IOException e) {
			logger.error(e);
		}
	}

	private void waitForInv() throws InterruptedException {
		while (isDisplayedWait()) {
			Thread.sleep(wait);
			logger.info(String.format("Waiting %dms for inventory...", wait));
		}
	}

	private boolean isDisplayedWait() {
		boolean displayed;
		try {
			displayed = driver.findElement(By.id("trade_inventory_pending")).isDisplayed();
		} catch (Exception e) {
			logger.error(e);
			return false;
		}
		return displayed;
	}

	private boolean nextPage() {
		WebElement elem = driver.findElement(By.id("pagebtn_next"));
		if (!elem.getAttribute("class").contains("disabled")) {
			elem.click();
			return true;
		}
		return false;
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
	}

}

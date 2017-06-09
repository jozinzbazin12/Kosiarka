package harvester.app.harvesters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import harvester.app.Argument;

public class STHarvester extends MetjmHarvester {

	public STHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap, "https://skin.trade/");
		logger.info("Using ST harvester");
	}

	@Override
	public void harvest(Map<Argument, String> args) throws InterruptedException {
		setWait(args.get(Argument.WAIT));
		setPriceLimit(args.get(Argument.MAX_PRICE));
		String pathToSave = args.get(Argument.PATH);
		waitForPageLoad();
		new FluentWait<>(driver).withTimeout(20, TimeUnit.SECONDS).pollingEvery(200, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class).until(ExpectedConditions.presenceOfElementLocated(By.id("maxCost")));
		new FluentWait<>(driver).withTimeout(20, TimeUnit.SECONDS).pollingEvery(200, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class).until(ExpectedConditions.presenceOfElementLocated(By.id("search_right")));

		new FluentWait<>(driver).withTimeout(20, TimeUnit.SECONDS).pollingEvery(200, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class)
				.until(ExpectedConditions.attributeToBeNotEmpty(driver.findElement(By.id("maxCost")), "value"));
		Thread.sleep(wait);
		WebElement max = driver.findElement(By.id("maxCost"));
		max.clear();
		new Actions(driver).moveToElement(max).click().sendKeys(String.valueOf((int) price)).perform();
		max.sendKeys(Keys.ENTER);

		WebElement search = driver.findElement(By.id("search_right"));
		search.sendKeys(args.get(Argument.ITEM));

		String waitMsg = String.format("Waitig %dms for items", wait);
		driver.findElement(By.id("refresh_bots_inventory")).click();
		while (driver.findElement(By.id("circle_bot_inventory")).isDisplayed()) {
			logger.info(waitMsg);
			Thread.sleep(wait);
		}

		driver.findElements(By.className("dagger")).forEach((i) -> i.click());
		List<WebElement> items = driver.findElements(
				By.xpath("//div[@class='offer_container_inventory_steam']//div[contains(@class, 'offer_container_invertory')]"));
		List<Item> urls = items.stream().map(i -> new Item(String.valueOf(System.currentTimeMillis()), i.getAttribute("inspect")))
				.filter(i -> i.getLink() != null).collect(Collectors.toList());
		collectScreens(pathToSave, urls, "st");
	}

	@Override
	public void login() throws FileNotFoundException, IOException {
	}

	@Override
	public void restoreSession() throws FileNotFoundException, IOException, ClassNotFoundException {
	}

	@Override
	public void buy(Map<Argument, String> args) throws InterruptedException {
	}

}

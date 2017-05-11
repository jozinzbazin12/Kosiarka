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

public class CMHarvester extends MetjmHarvester {

	public CMHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap, "https://cs.money/");
		logger.info("Using CM harvester");
	}

	@Override
	public void harvest(Map<Argument, String> args) throws InterruptedException {
		setWait(args.get(Argument.WAIT));
		setPriceLimit(args.get(Argument.MAX_PRICE));
		String pathToSave = args.get(Argument.PATH);
		new FluentWait<>(driver).withTimeout(20, TimeUnit.SECONDS).pollingEvery(200, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class).until(ExpectedConditions.presenceOfElementLocated(By.id("search_right")));
		waitForPageLoad();
		new FluentWait<>(driver).withTimeout(20, TimeUnit.SECONDS).pollingEvery(200, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class).until(ExpectedConditions.presenceOfElementLocated(By.id("search_right")));

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

		List<WebElement> items = driver.findElements(
				By.xpath("//div[@id='inventory_bots']//div[@class='invertory_container_links']//a[@class='invertory_link']"));
		List<Item> urls = items.stream().map(i -> new Item(String.valueOf(System.currentTimeMillis()), i.getAttribute(HREF)))
				.collect(Collectors.toList());
		collectScreens(pathToSave, urls, "cm");
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

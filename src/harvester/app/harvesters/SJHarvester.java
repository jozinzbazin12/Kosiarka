package harvester.app.harvesters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import harvester.app.Argument;

public class SJHarvester extends CountLimitedHarvester {

	public SJHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap, "http://skinsjar.com");
		logger.info("Using SJ harvester");
	}

	@Override
	public void harvest(Map<Argument, String> args) throws InterruptedException {
		setWait(args.get(Argument.WAIT));
		this.price = Double.parseDouble(args.get(Argument.MAX_PRICE));
		String pathToSave = args.get(Argument.PATH);

		WebElement max = driver.findElement(By.id("maxPrice"));
		max.clear();
		max.sendKeys(String.valueOf((int) price));

		WebElement search = driver.findElement(By.id("botSearch"));
		search.sendKeys(args.get(Argument.ITEM));

		String waitMsg = String.format("Waitig %dms for items", wait);
		driver.findElement(By.xpath("//img[@class='refresh ng-scope']")).click();
		while (driver.findElement(By.xpath("//img[@class='refresh ng-scope']")).getAttribute("ng-click") == null) {
			logger.info(waitMsg);
			Thread.sleep(wait);
		}

		List<WebElement> items = driver
				.findElements(By.xpath("//main[@id='botsInventoryContainer']//div[@class='item-steam-view ng-scope']//li[1]/a"));
		List<Item> urls = items.stream().map(i -> new Item(String.valueOf(System.currentTimeMillis()), i.getAttribute(HREF)))
				.collect(Collectors.toList());
		collectScreens(pathToSave, urls);
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

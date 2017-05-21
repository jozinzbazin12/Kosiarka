package harvester.app.harvesters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import harvester.app.Argument;

public class CSHarvester extends MetjmHarvester {

	private static final String INSPECTLINK = "inspectlink";

	public CSHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap, "https://csgosell.com/");
		logger.info("Using CS harvester");
	}

	@Override
	public void harvest(Map<Argument, String> args) throws InterruptedException {
		setWait(args.get(Argument.WAIT));
		setPriceLimit(args.get(Argument.MAX_PRICE));
		String pathToSave = args.get(Argument.PATH);
		new FluentWait<>(driver).withTimeout(20, TimeUnit.SECONDS).pollingEvery(200, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class).until(ExpectedConditions.presenceOfElementLocated(By.id("searchBar")));
		waitForPageLoad();
		new FluentWait<>(driver).withTimeout(20, TimeUnit.SECONDS).pollingEvery(200, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class).until(ExpectedConditions.presenceOfElementLocated(By.id("searchBar")));

		Thread.sleep(wait);
		WebElement max = driver.findElement(By.id("maxCost"));
		max.clear();
		max.sendKeys(String.valueOf((int) price) + "\n");

		WebElement search = driver.findElement(By.id("searchBar"));
		search.sendKeys(args.get(Argument.ITEM));

		driver.findElements(By.className("dagger")).forEach((i) -> i.click());

		String waitMsg = String.format("Waitig %dms for items", wait);
		driver.findElement(By.xpath("//div[@id='column2']//a[@class='refreshButton']")).click();
		while (driver.findElement(By.id("botTotalItems")).getText().isEmpty()) {
			logger.info(waitMsg);
			Thread.sleep(wait);
		}

		List<WebElement> items = driver.findElements(By.xpath("//div[@id='column2']//div[@class='itemImgDiv']")).parallelStream()
				.filter(elem -> elem.isDisplayed()).collect(Collectors.toList());
		List<Item> urls = items.parallelStream().filter(i -> !extracted(i).isEmpty())
				.map(i -> new Item(String.valueOf(System.currentTimeMillis()), extracted(i))).collect(Collectors.toList());
		collectScreens(pathToSave, urls, "cs");
	}

	private String extracted(WebElement i) {
		return i.getAttribute(INSPECTLINK);
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

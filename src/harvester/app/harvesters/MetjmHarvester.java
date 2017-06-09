package harvester.app.harvesters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Function;

import harvester.app.Argument;

public abstract class MetjmHarvester extends Harvester {

	protected int limit = 3;
	protected int pos = 0;
	protected int wait = 1000;
	protected double price = Double.MAX_VALUE;
	private boolean lite;

	protected static final String SESSION_DAT = "session.dat";

	public MetjmHarvester(Map<Argument, String> argumentMap, String url) {
		super(argumentMap, url);
		lite = argumentMap.containsKey(Argument.LITE);
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
			return Double.parseDouble(string);
		} catch (IllegalArgumentException e) {
			logger.error("Error while parsing price " + string, e);
		}
		return 0;
	}

	@Override
	protected boolean stopWhen() {
		return pos > limit;
	}

	private boolean doScreens(List<Item> items) throws InterruptedException {
		logger.debug(items);
		logger.info(String.format("Found %d items", items.size()));
		if (items.isEmpty()) {
			return false;
		}
		driver.get("https://metjm.net/csgo/");
		for (Item i : items) {
			driver.findElement(By.xpath("//div[@class='inspectLinkContainer']/input")).sendKeys(i.getLink());
			Thread.sleep(100);
			driver.findElement(By.xpath("//div[@class='inspectLinkContainer']/div")).click();
		}
		return true;
	}

	protected String collectScreens(String pathToSave, List<Item> items, String harvesterName) throws InterruptedException {
		boolean found = doScreens(items);
		if (lite) {
			logger.info("Press enter to finish...");
			if (found) {
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
		wait(items);

		List<WebElement> elements = driver.findElements(By.xpath("//div[@class='openImageButton' and @style]"));
		int id = elements.size() - 1;
		long time = System.currentTimeMillis();
		String path = pathToSave + "/" + time;
		if (harvesterName != null) {
			path += "_" + harvesterName;
		}
		for (Item i : items) {
			try {
				WebElement el = elements.get(id--);
				String attribute = el.getAttribute(STYLE);
				int urlpos = attribute.indexOf("background-image: url(\"");
				int endurlpos = attribute.indexOf("\");");
				String url = attribute.substring(urlpos + 31, endurlpos);
				url = "https://" + url.replace("_t.jpg", ".jpg");
				createSaveThread(url, path, i.getId() + ".jpg");
			} catch (StaleElementReferenceException e) {
				logger.error("Error while downloading " + i.getId(), e);
				driver.navigate().refresh();
				elements = driver.findElements(By.xpath("//div[@class='openImageButton' and @style]"));
			} catch (Exception e) {
				logger.error("Error while downloading " + i.getId(), e);
			}
		}
		return path;
	}

	private void wait(List<Item> items) {
		int count;
		int size = items.size();
		while ((count = driver.findElements(By.xpath("//div[@class='openImageButton' and @style]")).size()
				+ driver.findElements(By.xpath("//span[@id='patternIndexS']/text()[.='0']")).size()) < size) {
			try {
				String waitMsg = String.format("Waitig %dms for metjm (%d/%d) ...", wait, count, size);
				logger.info(waitMsg);
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void waitForPageLoad() {
		Wait<WebDriver> wait = new WebDriverWait(driver, 15);
		wait.until(new Function<WebDriver, Boolean>() {
			@Override
			public Boolean apply(WebDriver driver) {
				return String.valueOf(((JavascriptExecutor) driver).executeScript("return document.readyState"))
						.equals("complete");
			}
		});
	}

	protected void waitAndClick(By by) throws InterruptedException {
		while (!driver.findElement(by).isDisplayed()) {
			Thread.sleep(100);
			logger.info(String.format("Waiting %dms for element %s", 100, by));
		}
		driver.findElement(by).click();
	}

	protected void clickIfExists(By by) {
		try {
			driver.findElement(by).click();
		} catch (Exception e) {
			logger.info(String.format("Could not find %s, skipping...", by));
		}
	}

	protected void steamLogin() throws IOException, FileNotFoundException {
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
	protected void steamRestore() throws IOException, ClassNotFoundException, FileNotFoundException {
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
}

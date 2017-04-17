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
import org.openqa.selenium.WebElement;

import harvester.app.Argument;

public class LoungeHarvester extends CountLimitedHarvester {

	private static final String SESSION_DAT = "l_session.dat";

	private static final String LOUNGE = "https://csgolounge.com/mytrades";

	public LoungeHarvester(Map<Argument, String> argumentMap) {
		super(argumentMap, LOUNGE);
	}

	@Override
	public void buy(Map<Argument, String> args) throws InterruptedException {
	}

	@Override
	public void harvest(Map<Argument, String> args) throws InterruptedException {
		driver.get(LOUNGE);
		List<WebElement> elements = driver.findElements(By.xpath("//a[contains(@onclick,'bumpTrade')]"));
		logger.info(String.format("Found %d items to bump", elements.size()));
		for (WebElement e : elements) {
			e.click();
			Thread.sleep(wait);
		}
	}

	@Override
	public void login() throws FileNotFoundException, IOException {
		driver.get(LOUNGE);
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

}

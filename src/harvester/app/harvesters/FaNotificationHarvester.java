package harvester.app.harvesters;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import harvester.app.Argument;

public class FaNotificationHarvester extends CountLimitedHarvester {

	private static final String URL = "https://www.furaffinity.net/login/";

	public FaNotificationHarvester(String url, String login, String password) {
		super(URL, login, password);
	}

	@Override
	public void harvest(String pathToSave, Map<Argument, String> args) {
		driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
		driver.findElement(By.id("login")).sendKeys(args.get(Argument.LOGIN));
		WebElement inputPwd = driver.findElement(By.xpath("//input[@type='password']"));
		inputPwd.click();
		inputPwd.clear();
		String string = args.get(Argument.PASSWORD);
		inputPwd.sendKeys(string);

		driver.findElement(By.xpath("//input[@type='submit']")).click();

		driver.findElement(By.xpath("//li[@class='noblock']/a[@href='/msg/submissions/']")).click();

		setLimit(args.get(Argument.LIMIT));
		int i = 1;
		while (!stopWhen()) {
			driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);

			WebElement link = driver.findElement(By.xpath("(//s/a)[" + i++ + "]"));
			if (link == null) {
				driver.findElement(By.xpath("//div[@class='navigation']/a[@class='more']")).click();
				continue;
			}
			link.click();

			WebElement elem = driver.findElement(By.id("submissionImg"));

			createSaveThread(elem.getAttribute(SRC), pathToSave);
			driver.navigate().back();
			pos++;
		}
	}

}

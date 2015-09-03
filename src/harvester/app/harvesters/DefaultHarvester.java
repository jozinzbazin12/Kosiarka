package harvester.app.harvesters;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class DefaultHarvester extends Harvester {

	public DefaultHarvester(String url) {
		super(url);
	}

	@Override
	public void harvest(String path, int limit) {
		List<WebElement> elements = driver.findElements(By.xpath("//img"));

		int pos = 0;
		for (WebElement i : elements) {
			if (pos > limit) {
				break;
			}
			createSaveThread(i.getAttribute(SRC), path);
			pos++;
		}
	}

}

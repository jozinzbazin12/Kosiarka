package harvester.app.harvesters;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class _4Chan extends Harvester {

	public _4Chan(String url) {
		super(url);
	}

	@Override
	public void harvest(String pathToSave, int limit) {
		List<WebElement> elements = driver.findElements(By.xpath("//a[@class='fileThumb']"));
		int pos = 0;
		for (WebElement i : elements) {
			if (pos > limit) {
				break;
			}
			createSaveThread(i.getAttribute("href"), pathToSave);
			pos++;
		}
	}

}

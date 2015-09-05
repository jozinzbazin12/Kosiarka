package harvester.app.harvesters;

import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import harvester.app.Argument;

public class _4ChanHarvester extends CountLimitedHarvester {

	public _4ChanHarvester(String url) {
		super(url);
	}

	@Override
	public void harvest(String pathToSave, Map<Argument, String> args) {
		List<WebElement> elements = driver.findElements(By.xpath("//a[@class='fileThumb']"));
		setLimit(args.get(Argument.LIMIT));
		for (WebElement i : elements) {
			if (stopWhen()) {
				break;
			}
			createSaveThread(i.getAttribute("href"), pathToSave);
			pos++;
		}
	}

}

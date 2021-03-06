package harvester.app.harvesters;

import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import harvester.app.Argument;

public class DefaultHarvester extends CountLimitedHarvester {

	public DefaultHarvester(String url) {
		super(url);
	}

	@Override
	public void harvest(String path, Map<Argument, String> args) {
		List<WebElement> elements = driver.findElements(By.xpath("//img"));
		setLimit(args.get(Argument.LIMIT));
		for (WebElement i : elements) {
			if (stopWhen()) {
				break;
			}
			createSaveThread(i.getAttribute(SRC), path);
			pos++;
		}
	}

}

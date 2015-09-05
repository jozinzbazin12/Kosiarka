package harvester.app.harvesters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import harvester.app.Argument;

public class GoogleHarvester extends CountLimitedHarvester {

	public GoogleHarvester(String url) {
		super(url);
	}

	@Override
	public void harvest(String path, Map<Argument, String> args) {
		List<WebElement> elements = driver.findElements(By.className("rg_l"));
		List<WebElement> elems;
		List<String> set = new ArrayList<>();
		setLimit(args.get(Argument.LIMIT));

		for (WebElement i : elements) {
			if (stopWhen()) {
				break;
			}
			i.click();
			elems = driver.findElements(By.xpath("//div[@class='irc_c']//img[@class='irc_mi' and contains(@src, 'http')]"));
			for (WebElement j : elems) {
				String url = j.getAttribute(SRC);
				if (!set.contains(url)) {
					set.add(url);
					pos++;
				}
			}
		}
		for (String i : set) {
			createSaveThread(i, path);
		}
	}

}

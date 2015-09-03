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
		List<WebElement> elements = driver.findElements(By.xpath("//a[@class='fileThumb']/img"));
		int pos = 0;
		for (WebElement i : elements) {
			if (pos > limit) {
				break;
			}
			String src = i.getAttribute(SRC);
			int index = src.lastIndexOf(".") - 1;
			src = src.substring(0, index) + src.substring(index + 1);
			createSaveThread(src, pathToSave);
			pos++;
		}
	}

}

package harvester.app.harvesters;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

public abstract class Harvester {

	protected static final String SRC = "src";

	private static final String CONTENT_DISPOSITION = "Content-Disposition";

	private static final String USER_AGENT = "User-Agent";

	private static final String HEADER = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0";

	private static final int MAX_THREADS = 8;

	private int threads = 0;

	protected String url;

	protected static final Logger logger = Logger.getLogger(Harvester.class);

	protected WebDriver driver;

	public abstract void harvest(String pathToSave, int limit);

	protected String getFileName(String url, String raw) {
		if (raw != null && raw.indexOf("=") != -1) {
			String name = raw.split("=")[1];
			return name.replace("\"", "");
		}
		String str = url.substring(url.lastIndexOf("/") + 1, url.length());
		int index = str.indexOf("?");
		if (index > -1) {
			return str.substring(0, index);
		}
		return str;
	}

	private void saveImage(String fileUrl, String path) {
		URL url;
		InputStream is = null;
		OutputStream os = null;
		File file = null;
		logger.info("Downloading: " + fileUrl);
		try {
			File destination = new File(path);
			if (!destination.isDirectory()) {
				boolean dirDone = destination.mkdirs();
				if (!dirDone) {
					throw new IOException("Could not create destination directory");
				}
			}
			url = new URL(fileUrl);
			URLConnection con = url.openConnection();
			con.addRequestProperty(USER_AGENT, HEADER);
			String destinationFile = getFileName(fileUrl, con.getHeaderField(CONTENT_DISPOSITION));
			logger.info("Saving file: " + destinationFile);
			if ("gzip".equals(con.getContentEncoding())) {
				is = new GZIPInputStream(con.getInputStream());
			} else {
				is = con.getInputStream();
			}
			file = new File(destination.getAbsolutePath() + File.separator + destinationFile);
			if (file.exists()) {
				logger.info(MessageFormat.format("File [{0}] already exists.", file.getAbsolutePath()));
			}
			os = new FileOutputStream(file);

			byte[] b = new byte[10000];
			int length;

			while ((length = is.read(b)) != -1) {
				os.write(b, 0, length);
			}
		} catch (MalformedURLException e) {
			logger.error("Error with connection to URL: " + fileUrl, e);
		} catch (IOException e) {
			logger.error(MessageFormat.format("Error while saving file [{0}] from URL [{1}]", file, fileUrl), e);
		} finally {
			closeStream(is);
			closeStream(os);
		}
	}

	protected void createSaveThread(final String fileUrl, final String path) {
		while (threads >= MAX_THREADS) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				logger.error("Could not sleep thread");
			}
		}
		Thread t = new Thread() {
			@Override
			public void run() {
				saveImage(fileUrl, path);
				threads--;
			};
		};
		t.start();
		threads++;
	}

	protected void closeStream(Closeable c) {
		if (c == null) {
			return;
		}
		try {
			c.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Harvester(String url) {
		this.url = url;
		File pathToBinary = new File("C:\\Programy\\Mozilla Firefox\\firefox.exe");
		FirefoxBinary ffBinary = new FirefoxBinary(pathToBinary);
		FirefoxProfile firefoxProfile = new FirefoxProfile();
		driver = new FirefoxDriver(ffBinary, firefoxProfile);
		driver.get(url);
	}

	public void finish() {
		driver.close();
	}
}

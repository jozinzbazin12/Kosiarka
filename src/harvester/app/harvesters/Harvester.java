package harvester.app.harvesters;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import harvester.app.Argument;

public abstract class Harvester {

	private static final String CONTENT_DISPOSITION = "Content-Disposition";

	private static final String FIREFOX = "firefox";

	private static final String HEADER = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0";

	protected static final String HREF = "href";

	protected static final Logger logger = Logger.getLogger(Harvester.class);

	protected static final String SRC = "src";

	protected static final String STYLE = "style";

	private static final String USER_AGENT = "User-Agent";

	protected WebDriver driver;

	protected List<Future<String>> results = new ArrayList<>();

	protected ExecutorService service = Executors.newCachedThreadPool();

	protected String url;

	protected boolean logged;

	public Harvester(Map<Argument, String> argumentMap, String url) {
		this.url = url;
		String browser = argumentMap.get(Argument.BROWSER);
		if (browser != null && browser.equals(FIREFOX)) {
			System.setProperty(Argument.FIREFOX_BIN.getArg(), argumentMap.get(Argument.FIREFOX_BIN));
			System.setProperty(Argument.GECKO_DRIVER.getArg(), argumentMap.get(Argument.GECKO_DRIVER));
		}
		driver = new FirefoxDriver();
		if (url != null) {
			driver.get(url);
			try {
				restoreSession();
				logged = true;
			} catch (IOException | ClassNotFoundException e) {
				logger.debug(e);
				logger.error("Error while loading stored session");
			}
		}
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

	@SuppressWarnings("unchecked")
	protected void createSaveThread(final String fileUrl, final String path) {
		Future<String> submit = (Future<String>) service.submit(() -> saveImage(fileUrl, path, null));
		results.add(submit);
	}

	@SuppressWarnings("unchecked")
	protected void createSaveThread(final String fileUrl, final String path, String fileName) {
		Future<String> submit = (Future<String>) service.submit(() -> saveImage(fileUrl, path, fileName));
		results.add(submit);
	}

	public void finish() throws InterruptedException {
		if (!results.isEmpty()) {
			while (results.parallelStream().noneMatch(a -> a.isDone())) {
				logger.info("Waiting 5s for tasks to finish");
				Thread.sleep(5000);
				results.removeIf(this::finished);
			}
		}
		driver.quit();
		service.shutdown();
	}

	private boolean finished(Future<String> a) {
		try {
			return a.isDone() && a.get() != null;
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected String getFileName(String url, String raw) {
		for (int i = 0; i < 5; i++) {
			try {
				return getName(url, raw);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		logger.error("Could not obtain file name, using random");
		return String.valueOf(System.currentTimeMillis());
	}

	private String getName(String url, String raw) {
		if (raw != null && raw.indexOf("=") != -1) {
			String name = raw.split("=")[1];
			name = name.replace("\"", "");
			name = validateName(name);
			return name;
		}
		String str = url.substring(url.lastIndexOf("/") + 1, url.length());
		int index = str.indexOf("?");
		if (index > -1) {
			str = str.substring(0, index);
		}
		str = validateName(str);
		return str;
	}

	public abstract void buy(Map<Argument, String> args) throws InterruptedException;

	public abstract void harvest(Map<Argument, String> args) throws InterruptedException;

	public abstract void login() throws FileNotFoundException, IOException;

	public abstract void restoreSession() throws FileNotFoundException, IOException, ClassNotFoundException;

	private void saveImage(String fileUrl, String path, String fileName) {
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
			URL url = new URL(fileUrl);
			URLConnection con = url.openConnection();
			con.addRequestProperty(USER_AGENT, HEADER);
			if (fileName == null) {
				fileName = getFileName(fileUrl, con.getHeaderField(CONTENT_DISPOSITION));
			}
			logger.info("Saving file: " + fileName);
			if ("gzip".equals(con.getContentEncoding())) {
				is = new GZIPInputStream(con.getInputStream());
			} else {
				is = con.getInputStream();
			}
			file = new File(destination.getAbsolutePath() + File.separator + fileName);
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

	protected abstract boolean stopWhen();

	private String validateName(String name) {
		Pattern nameMatcher = Pattern.compile("[\\;\\\\\\/\\?\\<\\>\\\"\\|]");
		Matcher finder = nameMatcher.matcher(name);
		if (finder.find()) {
			logger.error("Invalid file name, using random");
			return String.valueOf(System.currentTimeMillis());
		}
		return name;
	}
}

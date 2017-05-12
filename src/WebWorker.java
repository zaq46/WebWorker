import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

import javafx.scene.chart.PieChart.Data;

public class WebWorker extends Thread {

	private final WebFrame frame;
	private final String INTERRUPTED = "Interrupted";
	private final String url;
	private final int rowNumber;

	public WebWorker(String url, int row, WebFrame frame) {

		this.url = url;
		this.rowNumber = row;
		this.frame = frame;
	}

	private void download() throws IOException {

		InputStream input = null;
		StringBuilder contents;

		try {

			URL u = new URL(url);
			URLConnection conncection = u.openConnection();
			conncection.connect();
			input = conncection.getInputStream();

			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			char[] array = new char[1000];
			int len;
			contents = new StringBuilder(1000);
			long start = System.currentTimeMillis();

			while ((len = br.read(array, 0, array.length)) > 0) {
				if (Thread.interrupted()) {
					frame.updateTable(rowNumber, INTERRUPTED);
				}
				contents.append(array, 0, len);
				Thread.sleep(100);
			}
			long end = System.currentTimeMillis();
			SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");
			Date date = new Date();
			String currentTime = df.format(date);
			String status = currentTime + "  " + (end - start) + "ms  " + contents.length() + " bytes";
			frame.updateTable(rowNumber, status);
		} catch (MalformedURLException e) {
			frame.updateTable(rowNumber, "err");
		} catch (InterruptedException e) {
			frame.updateTable(rowNumber, INTERRUPTED);
		} catch (IOException e) {
			frame.updateTable(rowNumber, "err");
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
			}
		}
	}

	public void run() {
		frame.increaseThreads();
		try {
			download();
		} catch (IOException e) {
			Logger.getLogger(WebWorker.class.getName()).log(Level.SEVERE, null, e);
		}
		frame.decreaseThreads();

	}
}

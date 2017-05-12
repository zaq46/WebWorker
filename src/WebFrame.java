import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import javax.swing.table.DefaultTableModel;

import com.sun.javafx.scene.control.SelectedCellsMap;

import javax.swing.*;

public class WebFrame extends JFrame {

	private final DefaultTableModel tableModel;
	private final JPanel panel;
	private final JTable table;
	private final JTextField field;
	private final JButton singleButton;
	private final JButton concurrentButton;
	private final JButton stopButton;
	private final JLabel running;
	private final JLabel completed;
	private final JLabel elapsed;
	private final JProgressBar progress;
	private int runningThreads;
	private Semaphore sem;
	private int completedTasks;
	private Thread time;
	private static String filePath;
	private ArrayList<WebWorker> worker;

	public WebFrame(String file) {

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		String[] column = new String[] { "url", "status" };
		tableModel = new DefaultTableModel(column, 0);
		table = new JTable(tableModel);
		JScrollPane scrollpane = new JScrollPane(table);
		scrollpane.setPreferredSize(new Dimension(600, 300));
		panel.add(scrollpane);

		field = new JTextField();
		field.setMaximumSize(new Dimension(50, JTextField.HEIGHT));
		

		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line = br.readLine();

			while (line != null) {
				tableModel.addRow(new String[] { line, "" });
				line = br.readLine();
			}
			br.close();

		} catch (FileNotFoundException e) {
			System.out.println("no valid file");
		} catch (IOException e) {
			System.out.println("IO Ecxeption");
		}

		concurrentButton = new JButton("Concurrent Fetch");
		concurrentButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (field.getText() != null) {
					concurrentButton.setEnabled(true);

					fetchButtonClicked();
					time = new Thread(new Launcher(Integer.parseInt(field.getText())));
					time.start();
				}
				
			}

		});

		singleButton = new JButton("Single Thread Fetch");
		singleButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fetchButtonClicked();
				time = new Thread(new Launcher(1));
				time.start();

			}
		});

		stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (time != null) {
					time.interrupt();
				}
				time = null;
				statusReset();
				singleButton.setEnabled(true);
				concurrentButton.setEnabled(true);
			}
		});

		stopButton.setEnabled(false);
		

		running = new JLabel("Running:0");
		completed = new JLabel("Completed:0");
		elapsed = new JLabel("Elapsed:0");
		progress = new JProgressBar();

		panel.add(singleButton);
		panel.add(concurrentButton);
		panel.add(field);
		panel.add(running);
		panel.add(completed);
		panel.add(elapsed);
		panel.add(progress);
		panel.add(stopButton);

		this.add(panel);
		this.pack();
		this.setVisible(true);

	}

	public void increaseThreads() {
		runningThreads++;
		updateRunning();
	}

	public synchronized void decreaseThreads() {
		runningThreads--;
		updateRunning();
		sem.release();
		;
	}

	private void updateRunning() {
		running.setText("Running: " + runningThreads);
	}

	public void updateTable(int r, String s) {
		increaseCompleted();
		progress.setValue(completedTasks);
		tableModel.setValueAt(s, r, 1);
	}

	public void increaseCompleted() {

		completedTasks++;
		completed.setText("Completed: " + completedTasks);
	}

	private void fetchButtonClicked() {
		statusReset();
		progress.setValue(0);
		running.setEnabled(true);
		completed.setEnabled(false);
		elapsed.setEnabled(false);
		progress.setMaximum(tableModel.getRowCount());
	}

	private void statusReset() {
		completedTasks = 0;
		progress.setValue(0);
		running.setText("Running: 0");
		completed.setText("Completed: 0");
		elapsed.setText("Elapsed: 0");
		progress.setValue(0);

	}

	private class Launcher implements Runnable {
		int workerLimit;

		public Launcher(int workerLimit) {
			this.workerLimit = workerLimit;
		}

		public void run() {
			long start = System.currentTimeMillis();
			runningThreads = 1;
			updateRunning();
			sem = new Semaphore(workerLimit);
			worker = new ArrayList<>();

			for (int i = 0; i < tableModel.getRowCount(); i++) {
				try {
					sem.acquire();
				} catch (InterruptedException e) {
					break;
				}
				WebWorker w = new WebWorker((String) tableModel.getValueAt(i, 0), i, WebFrame.this);
				worker.add(w);
				w.start();
			}
			for (int j = 0; j < worker.size(); j++) {
				try {
					worker.get(j).join();
				} catch (InterruptedException e) {
					break;
				}
			}
			runningThreads--;
			updateRunning();
			long end = System.currentTimeMillis();
			elapsed.setText("Elapsed: " + (end - start));
			stopButton.setEnabled(false);
			singleButton.setEnabled(true);
			concurrentButton.setEnabled(true);
		}

	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		filePath = args[0];
		WebFrame webFrame = new WebFrame(args[0]);
	}
}

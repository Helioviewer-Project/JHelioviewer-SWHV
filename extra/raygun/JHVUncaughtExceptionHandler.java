package org.helioviewer.jhv;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import com.mindscapehq.raygun4java.core.RaygunClient;

/**
 * Routines to catch and handle all runtime exceptions.
 */
public class JHVUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
{
	public static final JHVUncaughtExceptionHandler SINGLETON = new JHVUncaughtExceptionHandler();
	public static @Nullable String openGLRenderer = null;
	
	/**
	 * This method sets the default uncaught exception handler. Thus, this
	 * method should be called once when the application starts.
	 */
	public static void setupHandlerForThread()
	{
		Thread.setDefaultUncaughtExceptionHandler(JHVUncaughtExceptionHandler.SINGLETON);
	}

	/**
	 * Generates a simple error Dialog, allowing the user to copy the
	 * errormessage to the clipboard.
	 * <p>
	 * As options it will show {"Quit JHelioviewer", "Continue"} and quit if
	 * necessary.
	 * 
	 * @param title
	 *            Title of the Dialog
	 * @param msg
	 *            Object to display in the main area of the dialog.
	 */
	private static void showErrorDialog(final String title, final String msg, final Throwable e, final String log)
	{
		List<Object> objects = new ArrayList<>();

		JLabel head = new JLabel("Dang! You hit a bug in JHelioviewer.");
		head.setFont(head.getFont().deriveFont(Font.BOLD));

		objects.add(head);
		objects.add(Box.createVerticalStrut(10));
		objects.add(new JLabel("Here are some technical details about the problem:"));

		JTextArea textArea = new JTextArea();
		textArea.setMargin(new Insets(5, 5, 5, 5));
		textArea.setText(msg);
		textArea.setEditable(false);
		JScrollPane sp = new JScrollPane(textArea);
		sp.setPreferredSize(new Dimension(600, 400));

		objects.add(sp);
		JCheckBox allowCrashReport = new JCheckBox("Send this anonymous crash report to the developers.", JHVGlobals.IS_RELEASE_VERSION);
		objects.add(allowCrashReport);
		objects.add(Box.createVerticalStrut(10));

		JOptionPane optionPane = new JOptionPane(title);
		optionPane.setMessage(objects.toArray());
		optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
		optionPane.setOptions(new String[] { "Quit" });

		JFrame jf = new JFrame();
		jf.setUndecorated(true);
		jf.setLocationRelativeTo(null);
		jf.setIconImage(iconToImage(UIManager.getIcon("OptionPane.errorIcon")));
		jf.setVisible(true);

		JDialog dialog = optionPane.createDialog(jf, title);
		dialog.setAutoRequestFocus(true);
		dialog.setIconImage(iconToImage(UIManager.getIcon("OptionPane.errorIcon")));
		dialog.setResizable(true);
		dialog.setModalityType(ModalityType.TOOLKIT_MODAL);
		dialog.setVisible(true);

		jf.dispose();

		if (allowCrashReport.isSelected())
		{
			Map<String, String> customData = new HashMap<>();
			customData.put("Log", log);
			if(openGLRenderer!=null)
				customData.put("OpenGL", openGLRenderer);
			customData.put("JVM",
					System.getProperty("java.vm.name") + " "
							+ System.getProperty("java.vm.version") + " (JRE "
							+ System.getProperty("java.specification.version")
							+ ")");

            // send to raygun
            // TODO alternative crash-reporting
			RaygunClient client = new RaygunClient("+rfODAesOh4Col+swQzxYw==");
			client.SetVersion(JHVGlobals.version);
			client.Send(e, Arrays.asList(JHVGlobals.RAYGUN_TAG), customData);
			
		}

		Runtime.getRuntime().halt(0);
	}

	private JHVUncaughtExceptionHandler()
	{
	}

	public synchronized void uncaughtException(final @Nullable Throwable e)
	{
		uncaughtException(Thread.currentThread(), e);
	}
	
	
	static volatile boolean alreadyCaughtException=false;
	
	// we do not use the logger here, since it should work even before logging
	// initialization
	@SuppressWarnings("deprecation")
	public synchronized void uncaughtException(final @Nullable Thread t, final @Nullable Throwable e)
	{
		if(e==null)
			return;
		
		e.printStackTrace();
		
		// stop reentrant error reporting
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler()
		{
			@Override
			public void uncaughtException(@Nullable Thread _t, @Nullable Throwable _e)
			{
				// IGNORE all other exceptions
			}
		});

		if (!EventQueue.isDispatchThread())
		{
			try
			{
				EventQueue.invokeLater(() ->
				{
					try
					{
						uncaughtException(t, e);
					}
					catch (ThreadDeath _td)
					{
						// ignore concurrent, unhandled exceptions
					}
				});
				return;
			}
			catch (Exception _e)
			{
			}
		}

		// STOP THE WORLD to avoid exceptions piling up
		// Close all threads (excluding systemsthreads, just stop the timer
		// thread from the system)
		for (Thread thr : Thread.getAllStackTraces().keySet())
			if (thr != Thread.currentThread()
					&& (!thr.getThreadGroup().getName()
							.equalsIgnoreCase("system") || thr.getName()
							.contains("Timer")))
				try
				{
					thr.suspend();
				}
				catch (Throwable _th)
				{
				}
		for (Thread thr : Thread.getAllStackTraces().keySet())
			if (thr != Thread.currentThread()
					&& (!thr.getThreadGroup().getName()
							.equalsIgnoreCase("system") || thr.getName()
							.contains("Timer")))
				try
				{
					thr.stop();
				}
				catch (Throwable _th)
				{
				}

		final String finalLog = ""; //Log.GetLastFewLines(6);
		String msg = "JHelioviewer: " + JHVGlobals.version + "\n";
		msg += "Date: " + new Date() + "\n";
		msg += "JVM: " + System.getProperty("java.vm.name") + " "
				+ System.getProperty("java.vm.version") + " (JRE "
				+ System.getProperty("java.specification.version") + ")\n";
		msg += "OS: " + System.getProperty("os.name") + " "
				+ System.getProperty("os.arch") + " "
				+ System.getProperty("os.version") + "\n\n";
		msg += finalLog;

		try (StringWriter st = new StringWriter())
		{
			try (PrintWriter pw = new PrintWriter(st))
			{
				e.printStackTrace(pw);
				msg += st.toString();
			}
		}
		catch (IOException e1)
		{
		}

		for (Frame f : Frame.getFrames())
			f.setVisible(false);

		final String finalMsg = msg;
		
		if(alreadyCaughtException)
			return;

		alreadyCaughtException=true;

		// this wizardry forces the creation of a new awt event queue, if needed
		new Thread(() -> EventQueue.invokeLater(() -> JHVUncaughtExceptionHandler.showErrorDialog(
							"JHelioviewer: Fatal error", finalMsg, e,
							finalLog))).start();
	}

	private static Image iconToImage(Icon icon)
	{
		if (icon instanceof ImageIcon)
			return ((ImageIcon) icon).getImage();
		else
		{
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gd = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			BufferedImage image = gc.createCompatibleImage(w, h);
			Graphics2D g = image.createGraphics();
			icon.paintIcon(null, g, 0, 0);
			g.dispose();
			return image;
		}
	}
}

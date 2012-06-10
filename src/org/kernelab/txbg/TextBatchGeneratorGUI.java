package org.kernelab.txbg;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import org.kernelab.basis.Tools;

public class TextBatchGeneratorGUI extends JFrame
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1543844720687342664L;

	public static Border		SELECTED_BORDER		= new LineBorder(Color.WHITE, 3);

	public static Color Color(float[] color)
	{
		return new Color(color[0], color[1], color[2]);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		TextBatchGeneratorGUI app = new TextBatchGeneratorGUI();
		app.setVisible(true);
	}

	private TextBatchGenerator	txbg		= new TextBatchGenerator();

	private boolean				processing	= false;

	private JToggleButton		magic;

	private List<File>			files		= new LinkedList<File>();

	private int					interval	= 400;

	private int					steps		= 20;

	private Timer				shifter;

	private Timer				refresher;

	private int					polar		= 2;

	private float[][]			color		= new float[polar * 3][4];

	public TextBatchGeneratorGUI()
	{
		super("TxBG");
		this.config();
		this.arrange();
	}

	private void arrange()
	{
		this.add(magic);
		this.setBounds(120, 195, 180, 195);
	}

	private void config()
	{
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		txbg.gui(this);

		for (int i = 0; i < polar; i++) {
			for (int j = 0; j < 4; j++) {
				color[i][j] = (float) Math.random();
			}
		}

		magic = new JToggleButton("") {

			/**
			 * 
			 */
			private static final long	serialVersionUID	= 8839805022395936807L;

			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);

				paintMagic((Graphics2D) g);
			}
		};

		magic.setToolTipText("C, i am magic.");

		magic.setBorder(null);

		magic.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				if (magic.isSelected()) {
					files().clear();
					magic.setBorder(SELECTED_BORDER);
				} else {
					txbg.acceptDataFiles(files, true);
					magic.setBorder(null);
				}
			}

		});

		magic.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!processing() && !magic.isSelected()) {
					shifter.start();
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (!processing() && !magic.isSelected()) {
					shifter.stop();
					refresher.stop();
				}
			}
		});

		new DropTarget(magic, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {

			@SuppressWarnings("unchecked")
			public void drop(DropTargetDropEvent dtde)
			{
				if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {

					dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

					List<File> list = null;
					try {
						list = (List<File>) (dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));

						if (magic.isSelected()) {
							files.addAll(list);
						} else {
							txbg.acceptDataFiles(list, false);
						}

						dtde.dropComplete(true);

					} catch (UnsupportedFlavorException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

				} else {
					dtde.rejectDrop();
				}
			}
		});

		shifter = new Timer(interval, new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				shiftColor();
			}
		});

		refresher = new Timer(interval / steps, new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				refreshColor();
			}
		});
	}

	public List<File> files()
	{
		return files;
	}

	protected TextBatchGeneratorGUI files(List<File> files)
	{
		this.files = files;
		return this;
	}

	private void paintMagic(Graphics2D g)
	{
		g.setPaint(new GradientPaint(color[0][3] * magic.getWidth(), 0, Color(color[0]),
				color[1][3] * magic.getWidth(), magic.getHeight(), Color(color[1])));
		g.fillRect(0, 0, magic.getWidth(), magic.getHeight());
	}

	public synchronized boolean processing()
	{
		return processing;
	}

	protected TextBatchGeneratorGUI processing(boolean processing)
	{
		if (processing != this.processing) {
			this.processing = processing;
			if (processing) {
				magic.setEnabled(false);
				shifter.start();
			} else {
				shifter.stop();
				refresher.stop();
				magic.setEnabled(true);
			}
		}

		return this;
	}

	private void refreshColor()
	{
		for (int i = 0; i < polar; i++) {
			float[] source = color[i];
			float[] delta = color[i + 2];

			for (int j = 0; j < 4; j++) {
				source[j] = Tools.limitNumber(source[j] + delta[j], 0f, 1f);
				magic.repaint();
			}
		}
	}

	private void shiftColor()
	{
		for (int i = 0; i < polar; i++) {
			float[] source = color[i];
			float[] delta = color[i + 2];
			float[] target = color[i + 4];

			for (int j = 0; j < 4; j++) {
				target[j] = (float) Math.random();
				delta[j] = (target[j] - source[j]) / steps;
			}
		}
		refresher.start();
	}

	public TextBatchGenerator txbg()
	{
		return txbg;
	}
}

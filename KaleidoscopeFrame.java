import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class KaleidoscopeFrame implements MouseListener
{
	private static Kaleidoscope k;
	private static Thread kalThread;
	
	public static void main(String[] args)
	{
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run() 
			{
				init();
			}
		});
	}
	
	public static void init()
	{
		k = new Kaleidoscope();
		
		JFrame frame = new JFrame("Kaleidoscope");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(50, 50, 800, 700);
		frame.setResizable(false);
		Container c = frame.getContentPane();
		c.setBackground(Color.WHITE);
		c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
		
		c.add(Box.createRigidArea(new Dimension(0, 30)));
		c.add(buttonsPanel());
		c.add(k);
		frame.setVisible(true);
		
		kalThread = new Thread(k);
	}
	
	public static JPanel buttonsPanel()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.setBackground(Color.WHITE);
		
		JButton button1 = new JButton("Solve!");
		button1.setFont(new Font("TimesRoman", Font.PLAIN, 20));
		button1.setFocusable(false);
		button1.setBackground(Color.LIGHT_GRAY);
		button1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (k.showReference)
					kalThread.start();
			}
		});
		
		JButton button2 = new JButton("Reset");
		button2.setFont(new Font("TimesRoman", Font.PLAIN, 20));
		button2.setFocusable(false);
		button2.setBackground(Color.LIGHT_GRAY);
		button2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				kalThread.interrupt();
				k.reset();
				try {kalThread.join();}
				catch (InterruptedException e1) {}
				kalThread = new Thread(k);
			}
		});
		
		
		JToggleButton button3 = new JToggleButton("Set Pattern");
		button3.setFont(new Font("TimesRoman", Font.PLAIN, 20));
		button3.setFocusable(false);
		button3.setBackground(Color.LIGHT_GRAY);
		KaleidoscopeFrame kFrame = new KaleidoscopeFrame();
		button3.addActionListener(new ActionListener() {
			private boolean setPatternToggle = false;
			public void actionPerformed(ActionEvent e) {
				setPatternToggle = !setPatternToggle;
				if (setPatternToggle)
					k.addMouseListener(kFrame);
				else
					k.removeMouseListener(kFrame);
			}
		});
		
		JToggleButton button4 = new JToggleButton("Show Outlines");
		button4.setFont(new Font("TimesRoman", Font.PLAIN, 20));
		button4.setFocusable(false);
		button4.setBackground(Color.LIGHT_GRAY);
		button4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Piece.drawPieceOutlines = !Piece.drawPieceOutlines;
				k.repaint();
			}
		});
		
		JLabel delayLabel = new JLabel("Delay: ");
		delayLabel.setFont(new Font("TimesRoman", Font.PLAIN, 20));
		
		String[] delays = {"0 ms", "50 ms", "500 ms"};
		JComboBox<String> dropDown = new JComboBox<String>(delays);
		dropDown.setFocusable(false);
		dropDown.setMaximumSize(new Dimension(100, 50));
		dropDown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox<String> cb = (JComboBox<String>)e.getSource();
				String delay = ((String)cb.getSelectedItem());
	        	k.setDelay(Integer.parseInt(delay.substring(0, delay.length() - 3)));
			}
		});
		
		p.add(button2);
		p.add(Box.createRigidArea(new Dimension(30, 0)));
		p.add(button3);
		p.add(Box.createRigidArea(new Dimension(30, 0)));
		p.add(button1);
		p.add(Box.createRigidArea(new Dimension(30, 0)));
		p.add(button4);
		p.add(Box.createRigidArea(new Dimension(30, 0)));
		p.add(delayLabel);
		p.add(dropDown);
		
		return p;
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON1)
			k.changeReference(e.getPoint());
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
}
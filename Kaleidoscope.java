import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

/* v1:
 *		steps = 1,214,176,162
 * v2 (fixed rotation method so now it actually rotates):
 *		steps = 151,691
 * v3 (cycle through spots for first rotation, then second, etc. instead of cycling through rotations per spot):
 *		steps ~ 200,000 - incidental to specific code
 * v4 (v2 with heuristic of two or less 1x1 holes):
 *		steps = 123,531
 * v5: (v4 with consolidate canAddToBoard and addToBoard methods; also checks heuristics for 
 *			an added piece during same iteration instead of next recursive call)
 *		steps = 111,243
 * 
 * v11 - v1-5 all solved a standard checkerboard with the upper left corner red.
 * 	   - by inverting the checkerboard (black square in upper left corner), v5 takes steps = 2,667,732.
 * 	   - then, by inverting the primary position for the 8-line piece, v11 takes
 * 	   - steps = 654,960
 * v12 - v11 with fixed heuristic: now correctly identifies 1x1 squares on the first and last rows and columns
 *     - (previously only identified 1x1 squares NOT in these rows/columns)
 *     - steps = 275,568
 */


public class Kaleidoscope extends JPanel implements Runnable
{
	public static final int START_WIDTH = 100;	// beginning of grid
	public static final int START_HEIGHT = 50;	// beginning of grid
	public static final int SQ_SIZE = 35;		// for drawing
	
	private final char[] solutionColors = {'1', '2', '3', '4'};
	private char[][] board;
	private char[][] reference;
	
	private ArrayList<Piece> pieces;			// for red + black solution
	private ArrayList<Piece> coloredPieces;		// for colored map solution
	private boolean solved;
	private String solutionMessage;
	public volatile boolean showReference;
	private int numRed;
	private Image piecesImage;
	
	private int delayMS;		// for delaying after each newly drawn piece
	private long steps;

	public Kaleidoscope()
	{
		this.delayMS = 0;
		reference = new char[8][8];
		this.piecesImage = new ImageIcon(this.getClass().getResource("images/KalPieces.png")).getImage();
		reset();
	}
	
	// called by kThread.start(), makes this all work on another thread
	public void run()
	{
		board = new char[8][8];
		this.showReference = false;
		this.solved = false;
		this.steps = 0;
		repaint();
		if (!checkStartingHeuristics())
			notSolved();
		else
			placePiece(0);
		if (solved && !showReference)
		{
			for (Piece p : pieces)
				coloredPieces.add(p.getCopy());
			makeColorMap(0, 0, 4);
		}
	}
	
	public void reset()
	{
		this.solutionMessage = "";
		showReference = true;
		this.solved = false;
		this.steps = 0;
		System.out.println("reset");
		numRed = 32;
		
		pieces = new ArrayList<Piece>();			// make new array of pieces
		for (Piece.PieceType p : Piece.PieceType.values())
			pieces.add(new Piece(p));
		this.coloredPieces = new ArrayList<Piece>();

		for (int i = 0; i < reference.length; i++)	// initialize reference as standard chess board
		{
			for (int j = 0; j < reference[i].length; j++)
			{
				if (i%2 == j%2)
					reference[i][j] = 'b';
				else
					reference[i][j] = 'r';
			}
		}
		this.setVisible(true);
		repaint();
	}
	
	// called when user clicks on the grid; changes red square to black, black to red
	public void changeReference(Point p)
	{
		if (!this.showReference)
			return;		
		int i = ((int)p.getX() - START_WIDTH) / SQ_SIZE;
		int j = ((int)p.getY() - START_HEIGHT) / SQ_SIZE;
		if (i >= 0 && j >= 0 && i < 8 && j < 8)
		{
			if (reference[i][j] == 'r')
			{
				reference[i][j] = 'b';
				this.numRed--;
			}
			else
			{
				reference[i][j] = 'r';
				this.numRed++;
			}
		}
		repaint();
	}
	
	// depth-first search to place pieces to fit board and match colors of reference grid
	private void placePiece(int pieceIndex)
	{
		if (pieceIndex >= pieces.size())				// if all the pieces have been placed
		{
			solved();
			return;
		}
		Piece p = pieces.get(pieceIndex);				// otherwise:
		for (int i = 0; i < board.length; i++)			// for each place in the board
		{
			for (int j = 0; j < board[i].length; j++)
			{
				for (int k = 0; k < 4; k++)				// for each of four 90 degree piece rotations
				{
					if (showReference)
						return;
					steps++;
					boolean fitsLocation = p.addToBoard(board, i, j, reference);		// add the piece if it fits
					boolean fitsLocAndHeuristics = fitsLocation && checkHeuristics();	// check heuristics to make sure the add is ok
					if (fitsLocation)					// if it added (regardless of heuristics)
					{
						repaint();						// draw
						delay();						// wait
					}
					if (fitsLocAndHeuristics)			// if it added AND heuristics are ok
					{
						placePiece(pieceIndex+1);		// repeat for next piece
						if (this.solved)						// yaaay
							return;
					}
					if (fitsLocation)					// if it added then remove it
					{
						p.removeFromBoard(board, i, j);	// remove the piece
						repaint();						// draw
						delay();						// wait
					}
					p.rotateCW();						// rotate the piece
				}
			}
		}
		if (pieceIndex == 0)
			notSolved();
	}
	
	// heuristics to check at each node of placePiece search
	private boolean checkHeuristics()
	{
		// heuristic #1: two or less 1x1 holes exist
		int numSingleHoles = 0;
		for (int i = 0; i < board.length; i++)
			for (int j = 0; j < board[i].length; j++)
				if (board[i][j] == 0 && (j == board[i].length-1 || board[i][j+1] != 0) && (j == 0 || board[i][j-1] != 0)	// adjacent space either out of bounds or not empty
						&& (i == board.length-1 || board[i+1][j] != 0) && (i == 0 || board[i-1][j] != 0))
					numSingleHoles++;
		return (numSingleHoles <= 2);
	}
	
	// heuristics for identifying impossible configurations from the beginning before placePiece search
	private boolean checkStartingHeuristics()
	{
		// starting heuristic #1: if unequal # of spots of each color (ie numRed != 32), the config is unsolvable
		boolean uhoh = (this.numRed != 32);
		if (uhoh)
			this.solutionMessage = "Uneven number of red and black squares\n";
		return !(uhoh);
	}
	
	// depth-first search with backwards propagation to color a map of the solution
	// point = index of board (row*8+col); colorIndex = which color to try to paint with first; numColors = # of colors to try to paint the map with
	private boolean makeColorMap(int point, int colorIndex, int numColors)
	{
		if (point >= 64 || showReference)
			return true;
		Piece currPiece = getPieceAtLoc(point/8, point%8);					// get piece at current location
		while (point < 64 && getPieceAtLoc(point/8, point%8).isEqualTo(currPiece))	// increase point until it's at a new piece (new value used later)
			point++;

		ArrayList<Character> constraints = getAdjacentColors(currPiece);	// constraints are all the colors of adjacent pieces
		for (int i = 0; i < numColors - constraints.size(); i++)			// if constraints has size 3, only try the one remaining color. if size 2, try 2 remaining colors, etc.
		{																	// if constraint size == 4, skips this loop
			if (showReference)
				return true;
			while (constraints.contains(solutionColors[colorIndex]))		// go through solutionColors and look for a color not contained by constraints
				colorIndex = (colorIndex + 1) % numColors;
			currPiece.setColor(solutionColors[colorIndex]);						// color this piece the free color
			repaint();															// draw
			delay();															// delay
			if (makeColorMap(point, (colorIndex+1)%numColors, numColors))		// try to paint piece at next spot on board
				return true;														// if next piece painted successfully, we're done here boys
			colorIndex = (colorIndex + 1) % numColors;							// otherwise, this piece needs a different color
		}
		return false;
	}
	
	// returns the piece that contains the point (row, col)
	private Piece getPieceAtLoc(int row, int col)
	{
		for (Piece p : coloredPieces)
			for (ColoredPoint cp : p.getPoints())
				if (p.getRow() + cp.getIntX() == row && p.getCol() + cp.getIntY() == col)
					return p;
		return null;
	}
	
	// returns the colors of all adjacent points to Piece p
	private ArrayList<Character> getAdjacentColors(Piece p)
	{
		ArrayList<Character> adjCols = new ArrayList<Character>();
		for (ColoredPoint cp : p.getPoints())
		{
			int row = p.getRow() + cp.getIntX();
			int col = p.getCol() + cp.getIntY();
			while (row >= 0 && p.isEqualTo(getPieceAtLoc(row, col)))		// keep looking at previous rows for a different piece
				row--;
			if (row >= 0)
			{
				char temp = getPieceAtLoc(row, col).getColor();
				if (temp != 'r' && temp != 'b' && adjCols.indexOf(temp) == -1)
					adjCols.add(temp);
			}
			row = p.getRow() + cp.getIntX();
			while (col >= 0 && p.isEqualTo(getPieceAtLoc(row, col)))
				col--;
			if (col >= 0)
			{
				char temp = getPieceAtLoc(row, col).getColor();
				if (temp != 'r' && temp != 'b' && adjCols.indexOf(temp) == -1)
					adjCols.add(temp);
			}
			col = p.getCol() + cp.getIntY();
			while (row < 8 && p.isEqualTo(getPieceAtLoc(row, col)))
				row++;
			if (row < 8)
			{
				char temp = getPieceAtLoc(row, col).getColor();
				if (temp != 'r' && temp != 'b' && adjCols.indexOf(temp) == -1)
					adjCols.add(temp);
			}
			row = p.getRow() + cp.getIntX();
			while (col < 8 && p.isEqualTo(getPieceAtLoc(row, col)))
				col++;
			if (col < 8 && getPieceAtLoc(row, col) != null)
			{
				char temp = getPieceAtLoc(row, col).getColor();
				if (temp != 'r' && temp != 'b' && adjCols.indexOf(temp) == -1)
					adjCols.add(temp);
			}
		}
		return adjCols;
	}
	
	// called when the placePiece method wins
	private void solved()
	{
		this.solutionMessage = "Number of steps: " + String.format("%,d", this.steps);
		System.out.println(this.solutionMessage);
		this.solved = true;
		repaint();
	}
	
	private void notSolved()
	{
		this.solutionMessage += "No solutions found; Number of steps: " + String.format("%,d", this.steps);
		System.out.println(this.solutionMessage);
		repaint();
	}

	public void setDelay(int del)
	{
		this.delayMS = del;
	}
	
	private void delay()
	{
		try
		{Thread.sleep(this.delayMS);}
		catch (InterruptedException e) {}
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		drawBackground(g);
		drawGrids(g);
		if (this.showReference)
			drawReference(g);
		else
			for (Piece p : pieces)
				p.draw(g);
		if (this.solved)
			for (int i = 0; i < coloredPieces.size(); i++)
				coloredPieces.get(i).drawColorMap(g, this);
		if (!this.solutionMessage.equals(""))
			drawSteps(g);
	}
	
	private void drawGrids(Graphics g)
	{
		g.setColor(Color.BLUE);
		for (int i = 0; i < reference.length; i++)
		{
			for (int j = 0; j < reference[i].length; j++)
			{
				g.drawRect(i*SQ_SIZE + START_WIDTH, j*SQ_SIZE + START_HEIGHT, SQ_SIZE, SQ_SIZE);
				g.drawRect(this.getWidth() - START_WIDTH - SQ_SIZE - i*SQ_SIZE, j*SQ_SIZE + START_HEIGHT, SQ_SIZE, SQ_SIZE);
			}
		}
	}
	
	private void drawBackground(Graphics g)
	{
		int width = this.getWidth();
		int height = this.getHeight();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		g.drawImage(this.piecesImage, width/2 - this.piecesImage.getWidth(null)/2, 
				height - this.piecesImage.getHeight(null), null);
		g.setColor(Color.BLACK);
		g.setFont(new Font("TimesRoman", Font.PLAIN, 28));
		String x = "The Pieces: ";
		g.drawString(x, width/2 - 70, height - this.piecesImage.getHeight(null) - 20);
	}
	
	private void drawReference(Graphics g)
	{
		for (int i = 0; i < reference.length; i++)
		{
			for (int j = 0; j < reference[i].length; j++)
			{
				char temp = reference[i][j];
				if (temp == 'r')
					g.setColor(Color.RED);
				else if (temp == 'b')
					g.setColor(Color.BLACK);
				g.fillRect(i*SQ_SIZE + START_WIDTH, j*SQ_SIZE + START_HEIGHT, SQ_SIZE, SQ_SIZE);
			}
		}
		g.setFont(new Font("TimesRoman", Font.BOLD, 20));
		g.setColor(Color.BLACK);
		String sqColors = "Number of red squares: " + numRed + ", Number of black squares: " + (64-numRed);
		g.drawString(sqColors, START_WIDTH, 8*SQ_SIZE + START_HEIGHT + 22);
	}
	
	private void drawSteps(Graphics g)
	{
		g.setFont(new Font("TimesRoman", Font.BOLD, 20));
		g.setColor(Color.BLACK);
		int slashIndex = this.solutionMessage.indexOf("No solutions found");
		if (slashIndex <= 0)
			g.drawString(this.solutionMessage, START_WIDTH + 150, 8*SQ_SIZE + START_HEIGHT + 22);
		else
		{
			g.drawString(this.solutionMessage.substring(0, slashIndex), START_WIDTH + 150, 8*SQ_SIZE + START_HEIGHT + 22);
			g.drawString(this.solutionMessage.substring(slashIndex), START_WIDTH + 150, 8*SQ_SIZE + START_HEIGHT + 44);
		}
		g.drawString("One step = one spot check on board for one piece", START_WIDTH + 80, 8*SQ_SIZE + START_HEIGHT + 66);
	}
}
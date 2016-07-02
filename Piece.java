import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class Piece
{
	// INV = inversely colored
	// REV = reversed (mirrored)
	// ordered by size, biggest -> smallest
	public enum PieceType
	{
		LINE_8, LINE_4, SQUARE, L, L_INV, L_REV, L_REV_INV, T, 
		T_INV, Z, Z_REV, LINE_3, LINE_3_INV, CORNER, CORNER_INV,
		LINE_2, SINGLE_RED, SINGLE_BLACK;
	}

	public static boolean drawPieceOutlines = false;
	private PieceType type;
	private ArrayList<ColoredPoint> points;
	private boolean isOnBoard;		// whether it should be painted
	private int xPos, yPos;			// index of location of this piece's origin on board
	
	public Piece(PieceType typ)
	{
		type = typ;
		points = new ArrayList<ColoredPoint>();
		loadPieceCoords();
		this.isOnBoard = false;
	}
	
	public void setColor(char c)
	{
		for (ColoredPoint cp : points)
			cp.setColor(c);
	}
	
	// either black or red, or a Kaleidoscope solutionColor
	public char getColor()
	{
		return points.get(0).getColor();
	}
	
	public int getRow()
	{
		return xPos;
	}
	
	public int getCol()
	{
		return yPos;
	}
	public ArrayList<ColoredPoint> getPoints()
	{
		ArrayList<ColoredPoint> pointsCopy = new ArrayList<ColoredPoint>();
		for (ColoredPoint cp : points)
			pointsCopy.add(cp.getCopy());
		return pointsCopy;
	}
	
	public Piece getCopy()
	{
		Piece copy = new Piece(type);
		copy.isOnBoard = this.isOnBoard;
		copy.xPos = this.xPos;
		copy.yPos = this.yPos;
		copy.points = this.getPoints();
		return copy;
	}
	
	public boolean isEqualTo(Piece other)
	{
		if (other == null)
			return false;
		return (this.type == other.type);
	}
	
	// rotate clockwise
	public void rotateCW()
	{
		for (int i = 0; i < points.size(); i++)
		{
			ColoredPoint cp = points.get(i);
			points.set(i, new ColoredPoint(cp.getIntY(), -1*cp.getIntX(), cp.getColor())); // (x, y) --> (y, -x)
		}
	}
	
	// checks to see if there is space on the board for the piece and if the piece's colors match the reference matrix
	public boolean addToBoard(char[][] board, int i, int j, char[][] ref)
	{
		for (ColoredPoint cp : points)
		{
			int i2 = i+cp.getIntX();
			int j2 = j+cp.getIntY();
			if (i2 < 0 || j2 < 0 || i2 >= board.length || j2 >= board[0].length)	// if the point lies out of bounds
				return false;
			char temp = board[i2][j2];
			if (temp == 'r' || temp == 'b')											// if the point is already occupied
				return false;
			if (cp.getColor() != ref[i2][j2])										// if the point is the wrong color
				return false;
		}
		
		for (ColoredPoint cp : points)												// otherwise add the piece
		{
			board[i+cp.getIntX()][j+cp.getIntY()] = cp.getColor();
			this.isOnBoard = true;
			this.xPos = i;
			this.yPos = j;
		}
		return true;
	}
	
	public void removeFromBoard(char[][] board, int i, int j)
	{
		for (ColoredPoint cp : points)
			board[i+cp.getIntX()][j+cp.getIntY()] = 0;
		this.isOnBoard = false;
	}
	
	public void draw(Graphics g)
	{
		if (!isOnBoard)
			return;
		for (ColoredPoint cp : points)
		{
			// draw squares
			int cpX = cp.getIntX();
			int cpY = cp.getIntY();
			char cpColor = cp.getColor();
			if (cpColor == 'r')
				g.setColor(Color.RED);
			else if (cpColor == 'b')
				g.setColor(Color.BLACK);
			int xPixels = (xPos + cpX) * Kaleidoscope.SQ_SIZE + Kaleidoscope.START_WIDTH;
			int yPixels = (yPos + cpY) * Kaleidoscope.SQ_SIZE + Kaleidoscope.START_HEIGHT;
			g.fillRect(xPixels, yPixels, Kaleidoscope.SQ_SIZE, Kaleidoscope.SQ_SIZE);
			
			// draw outlines
			if (drawPieceOutlines)
				drawPieceOutlines(cpX, cpY, xPixels, yPixels, g);
		}
	}
	
	private void drawPieceOutlines(int cpX, int cpY, int xPixels, int yPixels, Graphics g)
	{
		g.setColor(Color.LIGHT_GRAY);
		boolean top = true;
		boolean left = true;
		boolean right = true;
		boolean bottom = true;
		for (ColoredPoint cp2 : points)
		{
			int cp2X = cp2.getIntX();	// check if any point from the same piece borders each point
			int cp2Y = cp2.getIntY();
			if (cpY + 1 == cp2Y)		// weird coords b/c of matrix to screen coord conversion
				bottom = false;
			if (cpY - 1 == cp2Y)
				top = false;
			if (cpX + 1 == cp2X)
				right = false;
			if (cpX - 1 == cp2X)
				left = false;
		}
		if (top)
			g.fillRect(xPixels, yPixels, Kaleidoscope.SQ_SIZE, 2);
		if (left)
			g.fillRect(xPixels, yPixels, 2, Kaleidoscope.SQ_SIZE);
		if (right)
			g.fillRect(xPixels + Kaleidoscope.SQ_SIZE - 2, yPixels, 2, Kaleidoscope.SQ_SIZE);
		if (bottom)
			g.fillRect(xPixels, yPixels + Kaleidoscope.SQ_SIZE - 2, Kaleidoscope.SQ_SIZE, 2);	
	}
	
	public void drawColorMap(Graphics g, Kaleidoscope k)
	{
		char temp = points.get(0).getColor();
		if (temp == '1')
			g.setColor(new Color(0x53299E));
		else if (temp == '2')
			g.setColor(new Color(0xCA2462));
		else if (temp == '3')
			g.setColor(new Color(0x7FD226));
		else if (temp == '4')
			g.setColor(new Color(0xE8D12A));
		else
			return;
		for (ColoredPoint cp : points)
		{
			int xPixels = k.getWidth() - Kaleidoscope.START_WIDTH - 8*Kaleidoscope.SQ_SIZE + (xPos + cp.getIntX()) * Kaleidoscope.SQ_SIZE;
			int yPixels = (yPos + cp.getIntY()) * Kaleidoscope.SQ_SIZE + Kaleidoscope.START_HEIGHT;
			g.fillRect(xPixels, yPixels, Kaleidoscope.SQ_SIZE, Kaleidoscope.SQ_SIZE);
		}
	}
	
	// 120 line method SUCH GOOD CODE
	private void loadPieceCoords()
	{
		if (type == PieceType.LINE_8)
		{
			for (int i = 0; i < 8; i++)
			{
				if (i%2 == 0)
					points.add(new ColoredPoint(i, 0, 'b'));
				else
					points.add(new ColoredPoint(i, 0, 'r'));
			}
		}
		else if (type == PieceType.LINE_3)
		{
			points.add(new ColoredPoint(0, 0, 'r'));
			points.add(new ColoredPoint(1, 0, 'b'));
			points.add(new ColoredPoint(2, 0, 'r'));
		}
		else if (type == PieceType.LINE_3_INV)
		{
			points.add(new ColoredPoint(0, 0, 'b'));
			points.add(new ColoredPoint(1, 0, 'r'));
			points.add(new ColoredPoint(2, 0, 'b'));
		}
		else if (type == PieceType.CORNER)
		{
			points.add(new ColoredPoint(0, 0, 'r'));
			points.add(new ColoredPoint(1, 0, 'b'));
			points.add(new ColoredPoint(1, 1, 'r'));
		}
		else if (type == PieceType.CORNER_INV)
		{
			points.add(new ColoredPoint(0, 0, 'b'));
			points.add(new ColoredPoint(1, 0, 'r'));
			points.add(new ColoredPoint(1, 1, 'b'));
		}
		else if (type == PieceType.L)
		{
			points.add(new ColoredPoint(0, 0, 'r'));
			points.add(new ColoredPoint(1, 0, 'b'));
			points.add(new ColoredPoint(2, 0, 'r'));
			points.add(new ColoredPoint(2, 1, 'b'));
		}
		else if (type == PieceType.L_INV)
		{
			points.add(new ColoredPoint(0, 0, 'b'));
			points.add(new ColoredPoint(1, 0, 'r'));
			points.add(new ColoredPoint(2, 0, 'b'));
			points.add(new ColoredPoint(2, 1, 'r'));
		}
		else if (type == PieceType.L_REV)
		{
			points.add(new ColoredPoint(0, 1, 'b'));
			points.add(new ColoredPoint(0, 0, 'r'));
			points.add(new ColoredPoint(1, 0, 'b'));
			points.add(new ColoredPoint(2, 0, 'r'));
		}
		else if (type == PieceType.L_REV_INV)
		{
			points.add(new ColoredPoint(0, 1, 'r'));
			points.add(new ColoredPoint(0, 0, 'b'));
			points.add(new ColoredPoint(1, 0, 'r'));
			points.add(new ColoredPoint(2, 0, 'b'));
		}
		else if (type == PieceType.LINE_2)
		{
			points.add(new ColoredPoint(0, 0, 'b'));
			points.add(new ColoredPoint(1, 0, 'r'));
		}
		else if (type == PieceType.LINE_4)
		{
			points.add(new ColoredPoint(0, 0, 'b'));
			points.add(new ColoredPoint(1, 0, 'r'));
			points.add(new ColoredPoint(2, 0, 'b'));
			points.add(new ColoredPoint(3, 0, 'r'));
		}
		else if (type == PieceType.SINGLE_BLACK)
		{
			points.add(new ColoredPoint(0, 0, 'b'));
		}
		else if (type == PieceType.SINGLE_RED)
		{
			points.add(new ColoredPoint(0, 0, 'r'));
		}
		else if (type == PieceType.SQUARE)
		{
			points.add(new ColoredPoint(0, 0, 'b'));
			points.add(new ColoredPoint(1, 0, 'r'));
			points.add(new ColoredPoint(0, 1, 'r'));
			points.add(new ColoredPoint(1, 1, 'b'));
		}
		else if (type == PieceType.T)
		{
			points.add(new ColoredPoint(0, 0, 'r'));
			points.add(new ColoredPoint(1, 0, 'b'));
			points.add(new ColoredPoint(1, 1, 'r'));
			points.add(new ColoredPoint(2, 0, 'r'));
		}
		else if (type == PieceType.T_INV)
		{
			points.add(new ColoredPoint(0, 0, 'b'));
			points.add(new ColoredPoint(1, 0, 'r'));
			points.add(new ColoredPoint(1, 1, 'b'));
			points.add(new ColoredPoint(2, 0, 'b'));
		}
		else if (type == PieceType.Z)
		{
			points.add(new ColoredPoint(0, 1, 'b'));
			points.add(new ColoredPoint(1, 1, 'r'));
			points.add(new ColoredPoint(1, 0, 'b'));
			points.add(new ColoredPoint(2, 0, 'r'));
		}
		else if (type == PieceType.Z_REV)
		{
			points.add(new ColoredPoint(0, 0, 'b'));
			points.add(new ColoredPoint(1, 0, 'r'));
			points.add(new ColoredPoint(1, 1, 'b'));
			points.add(new ColoredPoint(2, 1, 'r'));
		}
	}
	
	public String toString()
	{
		return type.toString();
	}
}
import java.awt.Point;

public class ColoredPoint extends Point
{
	private char color;
	
	public ColoredPoint(int x, int y, char col)
	{
		super(x, y);
		color = col;
	}
	
	public int getIntX()
	{
		return (int)super.getX();
	}
	
	public int getIntY()
	{
		return (int)super.getY();
	}
	
	public char getColor()
	{
		return color;
	}
	
	public void setColor(char c)
	{
		color = c;
	}
	
	public ColoredPoint getCopy()
	{
		return new ColoredPoint(this.getIntX(), this.getIntY(), this.getColor());
	}
}
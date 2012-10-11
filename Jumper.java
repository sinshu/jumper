import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

public class Jumper extends Applet implements KeyListener, Runnable
{
	static final int SCR_WIDTH = 256;
	static final int SCR_HEIGHT = 256;
	static final int CELLS_ROWS = 20;
	static final int CELLS_COLS = 20;
	static final int FIELD_WIDTH = CELLS_COLS * 32;
	static final int FIELD_HEIGHT = CELLS_ROWS * 32;
	
	static final int WALL = 1;
	static final int TOGE = 2;
	static final int BANE = 3;
	static final int EXIT = 4;
	
	static final int PLAY_WIDTH = 16;
	static final int PLAY_HEIGHT = 32;
	static final double PLAY_ACCEL = 0.5;
	static final double PLAY_POWER = 36;
	static final int PLAY_JUMPLIM = 4;
	static final double PLAY_GRAV = 0.625;
	static final double PLAY_MAXVX = 4;
	static final double PLAY_MAXVY = 16;
	
	static final double BANE_POWER = 16;
	
	static final int CANT_JUMP = 0;
	static final int CAN_JUMP = 1;
	static final int INIT_JUMP = 2;
	
	static final int GAME_OVER = 1;
	static final int GAME_CLEAR = 2;
	
	static final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz&+";
	
	int[][] cells;
	
	int startRow;
	int startCol;
	
	int endRow;
	int endCol;
	
	double playX;
	double playY;
	double playPrevX;
	double playPrevY;
	double playVX;
	double playVY;
	boolean playLeft;
	int playAnime;
	boolean playOnWall;
	int playJumpStat;
	int playJumpCnt;
	
	int gameStat;
	int gameOverCnt;
	
	double viewX;
	double viewY;
	double viewFix;
	
	boolean debug;
	
	boolean keyLeft;
	boolean keyRight;
	boolean keySpace;
	
	Image buffer;
	Image[] playl;
	Image[] playr;
	Image wall;
	Image toge;
	Image bane;
	Image exit;
	Image back;
	
	MediaTracker tracker;
	
	String query;
	
	public static void main(String args[])
	{
		(new Jumper()).init();
	}
	
	public void init()
	{
		cells = new int[CELLS_ROWS + 1][];
		for (int i = 0; i < CELLS_ROWS + 1; i++)
		{
			cells[i] = new int[CELLS_COLS];
			for (int j = 0; j < CELLS_COLS; j++)
			{
				if ((i == 0) || (i == CELLS_ROWS - 1) || (j == 0) || (j == CELLS_COLS - 1))
				{
					cells[i][j] = 1;
				}
				else
				{
					cells[i][j] = 0;
				}
			}
		}
		
		query = getDocumentBase().getQuery();
		if ((query != null) && checkQuery(query))
		{
			startRow = chars.indexOf(query.charAt(0));
			startCol = chars.indexOf(query.charAt(1));
			endRow = chars.indexOf(query.charAt(2));
			endCol = chars.indexOf(query.charAt(3));
			
			int temp[] = new int[(int)(Math.ceil(CELLS_ROWS * ((double)CELLS_COLS / 3)) * 3)];
			for (int i = 0; i < (int)(Math.ceil(CELLS_ROWS * ((double)CELLS_COLS / 3))); i++)
			{
				int n = chars.indexOf(query.charAt(i + 4));
				temp[i * 3] = n & 3;
				temp[i * 3 + 1] = (n & 12) >> 2;
				temp[i * 3 + 2] = (n & 48) >> 4;
			}
			for (int i = 0; i < CELLS_ROWS; i++)
			{
				for (int j = 0; j < CELLS_COLS; j++)
				{
					cells[i][j] = temp[i * CELLS_ROWS + j];
				}
			}
			cells[endRow][endCol] = EXIT;
		}
		else
		{
			startRow = 1;
			startCol = 1;
			endRow = 18;
			endCol = 18;
			cells[endRow][endCol] = EXIT;
		}
		
		debug = false;
		
		keyLeft = false;
		keyRight = false;
		keySpace = false;
		addKeyListener(this);
		
		buffer = createImage(SCR_WIDTH, SCR_HEIGHT);
		
		playl = new Image[5];
		playr = new Image[5];
		
		for (int i = 0; i < 5; i++)
		{
			playl[i] = Toolkit.getDefaultToolkit().getImage(getClass().getResource("playl" + (i + 1) + ".gif"));
			playr[i] = Toolkit.getDefaultToolkit().getImage(getClass().getResource("playr" + (i + 1) + ".gif"));
		}
		wall = Toolkit.getDefaultToolkit().getImage(getClass().getResource("wall.gif"));
		back = Toolkit.getDefaultToolkit().getImage(getClass().getResource("back.gif"));
		toge = Toolkit.getDefaultToolkit().getImage(getClass().getResource("toge.gif"));
		bane = Toolkit.getDefaultToolkit().getImage(getClass().getResource("bane.gif"));
		exit = Toolkit.getDefaultToolkit().getImage(getClass().getResource("exit.gif"));
		tracker = new MediaTracker(this);
		for (int i = 0; i < 5; i++)
		{
			tracker.addImage(playl[i], 1);
			tracker.addImage(playr[i], 1);
		}
		tracker.addImage(wall, 1);
		tracker.addImage(back, 1);
		tracker.addImage(bane, 1);
		tracker.addImage(exit, 1);
		try
		{
			tracker.waitForID(1);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		(new Thread(this)).start();
	}
	
	public void paint(Graphics g)
	{
		Graphics b = buffer.getGraphics();
		
		int drawX = (int)Math.round(viewX);
		int drawY = (int)Math.round(viewY);
		int backX = (int)Math.round(viewX / 2) % 32;
		int backY = (int)Math.round(viewY / 2) % 32;
		int limRows = (drawY + SCR_HEIGHT) / 32 + 1;
		int limCols = (drawX + SCR_WIDTH) / 32 + 1;
		for (int i = 0; i < SCR_HEIGHT / 32 + 1; i++)
		{
			for (int j = 0; j < SCR_WIDTH / 32 + 1; j++)
			{
				b.drawImage(back, j * 32 - backX, i * 32 - backY, this);
			}
		}
		for (int i = drawY / 32; (i < limRows) && (i < CELLS_ROWS); i++)
		{
			for (int j = drawX / 32; (j < limCols) && (j < CELLS_COLS); j++)
			{
				if (cells[i][j] == WALL)
				{
					b.drawImage(wall, j * 32 - drawX, i * 32 - drawY, this);
				}
				else if (cells[i][j] == TOGE)
				{
					b.drawImage(toge, j * 32 - drawX, i * 32 - drawY + 16, this);
				}
				else if (cells[i][j] == BANE)
				{
					b.drawImage(bane, j * 32 - drawX, i * 32 - drawY + 16, this);
				}
				else if (cells[i][j] == EXIT)
				{
					b.drawImage(exit, j * 32 - drawX + 4, i * 32 - drawY, this);
				}
			}
		}
		
		if (debug)
		{
			b.setColor(Color.gray);
			b.fillRect((int)Math.round(playPrevX - viewX), (int)Math.round(playPrevY - viewY), PLAY_WIDTH, PLAY_HEIGHT);
		}
		
		if (playLeft)
		{
			b.drawImage(playl[playAnime], (int)Math.round(playX - viewX), (int)Math.round(playY - viewY), this);
		}
		else
		{
			b.drawImage(playr[playAnime], (int)Math.round(playX - viewX), (int)Math.round(playY - viewY), this);
		}
		
		if (gameStat == GAME_OVER)
		{
			b.setColor(Color.black);
			b.drawString("‚l‚h‚r‚r", 17, 33);
			b.drawString("‚l‚h‚r‚r", 18, 33);
			b.setColor(Color.red);
			b.drawString("‚l‚h‚r‚r", 16, 32);
			b.drawString("‚l‚h‚r‚r", 17, 32);
		}
		else if (gameStat == GAME_CLEAR)
		{
			b.setColor(Color.black);
			b.drawString("‚n‚j", 17, 33);
			b.drawString("‚n‚j", 18, 33);
			b.setColor(Color.blue);
			b.drawString("‚n‚j", 16, 32);
			b.drawString("‚n‚j", 17, 32);
		}
		
		if (debug)
		{
			b.setColor(Color.white);
			b.drawString("playX = " + playX, 16, 32);
			b.drawString("playPrevX = " + playPrevX, 144, 32);
			b.drawString("playY = " + playY, 16, 48);
			b.drawString("playPrevY = " + playPrevY, 144, 48);
			b.drawString("playVX = " + playVX, 16, 64);
			b.drawString("dx = " + (playX - playPrevX), 144, 64);
			b.drawString("playVY = " + playVY, 16, 80);
			b.drawString("dy = " + (playY - playPrevY), 144, 80);
			b.drawString("playOnWall = " + playOnWall, 16, 96);
			b.drawString("playJumpStat = " + playJumpStat, 16, 112);
			b.drawString("playJumpCnt = " + playJumpCnt, 16, 128);
			b.drawString("playLeft = " + playLeft, 16, 144);
			b.drawString("playAnime = " + playAnime, 144, 144);
			if (getDocumentBase().getQuery() != null)
			{
				b.drawString(getDocumentBase().getQuery(), 16, 160);
			}
		}
		
		g.drawImage(buffer, 0, 0, this);
	}
	
	public void update(Graphics g)
	{
		paint(g);
	}
	
	public void keyTyped(KeyEvent e)
	{
	}
	
	public void keyPressed(KeyEvent e)
	{
		int code = e.getKeyCode();
		if (code == KeyEvent.VK_LEFT)
		{
			keyLeft = true;
		}
		else if (code == KeyEvent.VK_RIGHT)
		{
			keyRight = true;
		}
		else if (code == KeyEvent.VK_SPACE)
		{
			keySpace = true;
		}
		else if (code == KeyEvent.VK_D)
		{
			debug = true;
		}
	}
	
	public void keyReleased(KeyEvent e)
	{
		int code = e.getKeyCode();
		if (code == KeyEvent.VK_LEFT)
		{
			keyLeft = false;
		}
		else if (code == KeyEvent.VK_RIGHT)
		{
			keyRight = false;
		}
		else if (code == KeyEvent.VK_SPACE)
		{
			keySpace = false;
		}
		else if (code == KeyEvent.VK_D)
		{
			debug = false;
		}
	}
	
	public void run()
	{
		initGame();
		
		while (true)
		{
			stepGame(keyLeft, keyRight, keySpace);
			repaint();
			try
			{
				if (debug)
				{
					Thread.sleep(1000);
				}
				else
				{
					Thread.sleep(33);
				}
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	public static boolean checkQuery(String s)
	{
		if (s.length() != (int)Math.ceil(CELLS_ROWS * ((double)CELLS_COLS / 3)) + 4)
		{
			return false;
		}
		for (int i = 0; i < s.length(); i++)
		{
			if (chars.indexOf(s.charAt(i)) == -1)
			{
				return false;
			}
		}
		return true;
	}
	
	public void initGame()
	{
		playX = startCol * 32 + 8;
		playY = startRow * 32;
		playPrevX = playX;
		playPrevY = playY;
		playVX = 0;
		playVY = 0;
		playLeft = false;
		playAnime = 0;
		playOnWall = false;
		playJumpStat = CAN_JUMP;
		playJumpCnt = 0;
		viewX = playX - SCR_WIDTH / 2 + PLAY_WIDTH / 2;
		viewY = playY - SCR_HEIGHT / 2 + PLAY_HEIGHT / 2 - 16;
		viewFix = 0;
		gameStat = 0;
		gameOverCnt = 0;
	}
	
	public void stepGame(boolean goLeft, boolean goRight, boolean jump)
	{
		if ((gameStat == GAME_OVER) || (gameStat == GAME_CLEAR))
		{
			if (gameOverCnt < 90)
			{
				gameOverCnt++;
			}
			else
			{
				initGame();
			}
		}
		
		if (!goLeft && !goRight)
		{
			playAnime = 0;
		}
		if (goLeft && !goRight && (gameStat == 0))
		{
			playVX -= PLAY_ACCEL * 2;
			playLeft = true;
			playAnime++;
			playAnime %= 4;
		}
		else if (goRight && !goLeft && (gameStat == 0))
		{
			playVX += PLAY_ACCEL * 2;
			playLeft = false;
			playAnime++;
			playAnime %= 4;
		}
		
		if (!playOnWall)
		{
			playAnime = 0;
		}
		
		if (gameStat == GAME_CLEAR)
		{
			playAnime = 4;
		}
		
		if (!jump && (playJumpStat == CANT_JUMP) && (playOnWall || (playVY > 0)))
		{
			playJumpStat = CAN_JUMP;
		}
		if (jump && playOnWall && (playJumpStat == CAN_JUMP))
		{
			playJumpStat = INIT_JUMP;
			playJumpCnt = 0;
		}
		if (jump && (playJumpStat == INIT_JUMP) && (gameStat == 0))
		{
			if ((playJumpCnt >= PLAY_JUMPLIM) || !playOnWall)
			{
				playVY = -Math.sqrt(PLAY_POWER * playJumpCnt);
				playJumpStat = CANT_JUMP;
				playJumpCnt = 0;
			}
			else if (playJumpCnt < PLAY_JUMPLIM)
			{
				playJumpCnt++;
			}
		}
		if (!jump && (playJumpStat == INIT_JUMP) && (gameStat == 0))
		{
			playVY = -Math.sqrt(PLAY_POWER * playJumpCnt);
			playJumpStat = CANT_JUMP;
			playJumpCnt = 0;
		}
		
		playVY += PLAY_GRAV;
		if (Math.abs(playVX) < PLAY_ACCEL)
		{
			playVX = 0;
		}
		else
		{
			if (playVX < 0)
			{
				playVX += PLAY_ACCEL;
			}
			else if (playVX > 0)
			{
				playVX -= PLAY_ACCEL;
			}
		}
		if (playVX < -PLAY_MAXVX)
		{
			playVX = -PLAY_MAXVX;
		}
		else if (playVX > PLAY_MAXVX)
		{
			playVX = PLAY_MAXVX;
		}
		if (playVY > PLAY_MAXVY)
		{
			playVY = PLAY_MAXVY;
		}
		
		playPrevX = playX;
		playPrevY = playY;
		playX += playVX;
		playY += playVY;
		
		if (playX < 0)
		{
			playX = 0;
			playVX = 0;
		}
		else if (playX > FIELD_WIDTH - PLAY_WIDTH)
		{
			playX = FIELD_WIDTH - PLAY_WIDTH;
			playVX = 0;
		}
		if (playY < 0)
		{
			playY = 0;
			playVY = 0;
		}
		else if (playY > FIELD_HEIGHT)
		{
			playY = FIELD_HEIGHT;
			playVX = 0;
			playVY = 0;
			gameStat = GAME_OVER;
		}
		
		playOnWall = false;
		// ¶ã‚ß‚èž‚ÝÌ×¸Þ
		boolean ul = cells[(int)(playY / 32)][(int)(playX / 32)] == WALL;
		// ‰Eã‚ß‚èž‚ÝÌ×¸Þ
		boolean ur = cells[(int)(playY / 32)][(int)(Math.ceil((playX + PLAY_WIDTH) / 32)) - 1] == WALL;
		// ¶‰º‚ß‚èž‚ÝÌ×¸Þ
		boolean ll = cells[(int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1][(int)(playX / 32)] == WALL;
		// ‰E‰º‚ß‚èž‚ÝÌ×¸Þ
		boolean lr = cells[(int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1][(int)(Math.ceil((playX + PLAY_WIDTH) / 32)) - 1] == WALL;
		
		// ‰E‚ß‚èž‚ÝÁª¯¸
		if (ur && lr)
		{
			playX = (int)((playX + PLAY_WIDTH) / 32) * 32 - PLAY_WIDTH;
			playVX = 0;
		}
		
		// ‰Eã‚ß‚èž‚ÝÁª¯¸
		if (ur && !ul && !lr)
		{
			// ‰º‚©‚ç·À
			if ((Math.ceil((playPrevX + PLAY_WIDTH) / 32) == Math.ceil((playX + PLAY_WIDTH) / 32)) && ((int)(playPrevY / 32) == (int)(playY / 32) + 1))
			{
				playY = ((int)(playY / 32) + 1) * 32;
				playVY = 0;
			}
			// ¶‰º‚©¶‚©‚ç·À
			else
			{
				playX = (int)((playX + PLAY_WIDTH) / 32) * 32 - PLAY_WIDTH;
				playVX = 0;
			}
		}
		
		// ã‚ß‚èž‚ÝÁª¯¸
		if (ul && ur)
		{
			playY = ((int)(playY / 32) + 1) * 32;
			playVY = 0;
		}
		
		// ¶ã‚ß‚èž‚ÝÁª¯¸
		if (ul && !ur && !ll)
		{
			// ‰º‚©‚ç·À
			if (((int)(playPrevX / 32) == (int)(playX / 32)) && ((int)(playPrevY / 32) == (int)(playY / 32) + 1))
			{
				playY = ((int)(playY / 32) + 1) * 32;
				playVY = 0;
			}
			// ‰E‰º‚©‰E‚©‚ç·À
			else
			{
				playX = ((int)(playX / 32) + 1) * 32;
				playVX = 0;
			}
		}
		
		// ¶‚ß‚èž‚ÝÁª¯¸
		if (ul && ll)
		{
			playX = ((int)(playX / 32) + 1) * 32;
			playVX = 0;
		}
		
		// ¶‰º‚ß‚èž‚ÝÁª¯¸
		if (ll && !ul && !lr)
		{
			// ã‚©‚ç·À
			if (((int)(playPrevX / 32) == (int)(playX / 32)) && ((int)(Math.ceil((playPrevY + PLAY_HEIGHT) / 32)) == (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1))
			{
				playY = (int)((playY + PLAY_HEIGHT) / 32) * 32 - PLAY_HEIGHT;
				playVY = 0;
				playOnWall = true;
			}
			// ‰Eã‚©‚ç·À
			else if (((int)(playPrevX / 32) == (int)(playX / 32) + 1) && ((int)(Math.ceil((playPrevY + PLAY_HEIGHT) / 32)) == (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1))
			{
				playY = (int)((playY + PLAY_HEIGHT) / 32) * 32 - PLAY_HEIGHT;
				playVY = 0;
				playOnWall = true;
			}
			// ‰E‚©‚ç·À
			else
			{
				playX = ((int)(playX / 32) + 1) * 32;
				playVX = 0;
			}
		}
		
		// ‰º‚ß‚èž‚ÝÁª¯¸
		if (ll && lr)
		{
			playY = (int)((playY + PLAY_HEIGHT) / 32) * 32 - PLAY_HEIGHT;
			playVY = 0;
			playOnWall = true;
		}
		
		// ‰E‰º‚ß‚èž‚ÝÁª¯¸
		if (lr && !ur && !ll)
		{
			// ã‚©‚ç·À
			if (((int)(Math.ceil((playPrevX + PLAY_WIDTH) / 32)) == (int)(Math.ceil((playX + PLAY_WIDTH) / 32))) && ((int)(Math.ceil((playPrevY + PLAY_HEIGHT) / 32)) == (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1))
			{
				playY = (int)((playY + PLAY_HEIGHT) / 32) * 32 - PLAY_HEIGHT;
				playVY = 0;
				playOnWall = true;
			}
			// ¶ã‚©‚ç·À
			else if (((int)(Math.ceil((playPrevX + PLAY_WIDTH) / 32)) == (int)(Math.ceil((playX + PLAY_WIDTH) / 32)) - 1) && ((int)(Math.ceil((playPrevY + PLAY_HEIGHT) / 32)) == (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1))
			{
				playY = (int)((playY + PLAY_HEIGHT) / 32) * 32 - PLAY_HEIGHT;
				playVY = 0;
				playOnWall = true;
			}
			// ¶‚©‚ç·À
			else
			{
				playX = (int)((playX + PLAY_WIDTH) / 32) * 32 - PLAY_WIDTH;
				playVX = 0;
			}
		}
		
		// ¶‰ºÄ¹ÞÁª¯¸
		if (cells[(int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1][(int)(playX / 32)] == TOGE)
		{
			if ((playY > (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) * 32 - 16 - PLAY_HEIGHT) && (playPrevY <= (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) * 32 - 16 - PLAY_HEIGHT))
			{
				playY = (int)Math.ceil((playY + PLAY_HEIGHT) / 32) * 32 - 16 - PLAY_HEIGHT;
				playVX = 0;
				playVY = 0;
				gameStat = GAME_OVER;
			}
		}
		//‰E‰ºÄ¹ÞÁª¯¸
		if (cells[(int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1][(int)(Math.ceil((playX + PLAY_WIDTH) / 32)) - 1] == TOGE)
		{
			if ((playY > (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) * 32 - 16 - PLAY_HEIGHT) && (playPrevY <= (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) * 32 - 16 - PLAY_HEIGHT))
			{
				playY = (int)Math.ceil((playY + PLAY_HEIGHT) / 32) * 32 - 16 - PLAY_HEIGHT;
				playVX = 0;
				playVY = 0;
				gameStat = GAME_OVER;
			}
		}
		
		// ¶‰ºÊÞÈÁª¯¸
		if (cells[(int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1][(int)(playX / 32)] == BANE)
		{
			if ((playY > (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) * 32 - 16 - PLAY_HEIGHT) && (playPrevY <= (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) * 32 - 16 - PLAY_HEIGHT))
			{
				playY = (int)Math.ceil((playY + PLAY_HEIGHT) / 32) * 32 - 16 - PLAY_HEIGHT;
				playVY = -BANE_POWER;
			}
		}
		//‰E‰ºÊÞÈÁª¯¸
		if (cells[(int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) - 1][(int)(Math.ceil((playX + PLAY_WIDTH) / 32)) - 1] == BANE)
		{
			if ((playY > (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) * 32 - 16 - PLAY_HEIGHT) && (playPrevY <= (int)(Math.ceil((playY + PLAY_HEIGHT) / 32)) * 32 - 16 - PLAY_HEIGHT))
			{
				playY = (int)Math.ceil((playY + PLAY_HEIGHT) / 32) * 32 - 16 - PLAY_HEIGHT;
				playVY = -BANE_POWER;
			}
		}
		
		if ((Math.abs(endCol * 32 + 16 - playX - PLAY_WIDTH / 2) <= 4) && (playY == endRow * 32) && playOnWall)
		{
			playX = endCol * 32 + 8;
			playVX = 0;
			playVY = 0;
			gameStat = GAME_CLEAR;
		}
		
		if (playLeft)
		{
			viewFix--;
		}
		else
		{
			viewFix++;
		}
		if (viewFix < -48)
		{
			viewFix = -48;
		}
		if (viewFix > 48)
		{
			viewFix = 48;
		}
		viewX = playX - SCR_WIDTH / 2 + PLAY_WIDTH / 2 + viewFix;
		if (gameStat == 0)
		{
			viewY = playY - SCR_HEIGHT / 2 + PLAY_HEIGHT / 2 - 16;
		}
		else
		{
			viewY--;
		}
		if (viewX < 0)
		{
			viewX = 0;
		}
		if (viewX > FIELD_WIDTH - SCR_WIDTH)
		{
			viewX = FIELD_WIDTH - SCR_WIDTH;
		}
		if (viewY < 0)
		{
			viewY = 0;
		}
		if (viewY > FIELD_HEIGHT - SCR_HEIGHT)
		{
			viewY = FIELD_HEIGHT - SCR_HEIGHT;
		}
	}
}

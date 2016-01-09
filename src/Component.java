import java.applet.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;

public class Component extends Applet implements Runnable {

	public static int ticksPerSecond = 60;

	public static int totalTicks = 0;

	public static int destructionTick = -1;

	static int width = 700;
	static int height = 750;

	private static final long serialVersionUID = 1L;

	public static Dimension size = new Dimension(width, height);

	public static String name = "Puzzle";

	public static boolean isRunning = false;

	public static Point screenPos = new Point(-1, -1);

	public static Point mousePos = null;

	private Image screen;

	public static int tileSize = 100;

	public static int newBonus = 0;
	public static int numBonus = 0;

	public static int destroyTick = -1;
	public static int maxDestroyTick = 15;

	public static Point clearPoint = new Point(0, 0);

	public static int movingLength = 0;
	public static Point direction = new Point(0, 0);

	public static boolean gamePaused = false;
	public static boolean gamePlayable = true;

	public static Color colors[] = { null, new Color(50, 50, 200), new Color(200, 120, 30), new Color(200, 50, 50), new Color(200, 200, 50), new Color(50, 200, 50), new Color(50, 150, 200), /* new Color(150, 50, 200) */}; // (first color = no draw)

	public static int bonusPiece = colors.length;

	public static int explosivePiece = colors.length + 1;

	public static ArrayList<Point> explosionLocation = new ArrayList<Point>();
	public static int explosionTick = -1;

	public static ArrayList<DisplayStrings> displayStrings = new ArrayList<DisplayStrings>();

	public static int lastColors[] = new int[10];

	public static int numSame = 0;

	public static int numCleared = 0;
	public static int numCombo = 0;

	public static long score = 0;
	public static long fakeScore = 0;

	public static int frameNum = 0;
	public static int maxFrame = 10;

	public static BufferedImage backGround;
	public static BufferedImage explosionAnimation;
	public static BufferedImage explosivePieceImage;
	public static BufferedImage bonusPieceImage;
	public static BufferedImage circlesImage;
	public static BufferedImage fgScoreImage;
	public static BufferedImage bgScoreImage;
	public static BufferedImage allBlockersClearedImage;
	public static BufferedImage allClearedImage;

	// Modifiable Game Variables
	public static final int s = 7; // size, (has to be a square)
	public static final int DESTRUCTION_TIMER = ticksPerSecond * 90;
	public static final int SHAKE_TICK = ticksPerSecond * 10;
	public static final int COMBO_REQUIREMENT = 4;
	public static final int COMBO_REQUIREMENT_BONUS = 6;
	public static final int MAX_COMBO = 6;
	public static final int CLEAR_ALL = 1000000;
	public static final int CLEAR_ALL_BONUS = 5000000;
	public static final int CLEAR_BONUS_PIECE = 100000;

	public static int grid[][] = new int[s][s];
	public static Point offSet[][] = new Point[s][s];
	public static int totalInCombo[][] = new int[s][s];
	public static boolean dropping[][] = new boolean[s][s];
	public static boolean destroying[][] = new boolean[s][s];

	public Component() {
		setPreferredSize(size);
		addKeyListener(new Listening());
		addMouseListener(new Listening());
		addMouseWheelListener(new Listening());
		try {
			backGround = ImageIO.read(new File("res/Background.png"));
			explosionAnimation = ImageIO.read(new File("res/ExplosionAnimation.png"));
			explosivePieceImage = ImageIO.read(new File("res/ExplosivePiece.png"));
			bonusPieceImage = ImageIO.read(new File("res/BonusPiece.png"));
			circlesImage = ImageIO.read(new File("res/Color.png"));
			fgScoreImage = ImageIO.read(new File("res/FgScore.png"));
			bgScoreImage = ImageIO.read(new File("res/BgScore.png"));
			allBlockersClearedImage = ImageIO.read(new File("res/AllBlockersCleared.png"));
			allClearedImage = ImageIO.read(new File("res/AllClearedBG.png"));
		} catch (Exception e) {
		}
	}

	public void start() {

		// Defining Objects

		for (int x = 0; x < s; x++) {
			for (int y = 0; y < s; y++) {
				grid[x][y] = new Random().nextInt(colors.length - 1) + 1;
				offSet[x][y] = new Point(0, 0);
				totalInCombo[x][y] = 0;
				destroying[x][y] = false;
			}
		}

		for (int i = 0; i < lastColors.length; i++) {
			lastColors[i] = 0;
		}

		updateBoard();

		// Starting game loop
		isRunning = true;

		new Thread(this).start();
	}

	public void stop() {
		isRunning = false;
	}

	public static void main(String args[]) {

		Component component = new Component();

		JFrame frame = new JFrame();
		frame.setTitle(name);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(component);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		// frame.setIconImage(icon);

		component.start();
	}

	public void tick() {

		// Fake score doesn't go up in value instantly
		if (fakeScore < score) {
			if ((score - fakeScore) / 60 > 100000) { // Max uprate
				fakeScore += 100000;
			} else {
				fakeScore += (score - fakeScore) / 60;
			}
			fakeScore += 5000; // Min uprate
			if (fakeScore > score) {
				fakeScore = score;
			}
		}

		for (int i = 0; i < displayStrings.toArray().length; i++) {
			displayStrings.get(i).tick();
			if (displayStrings.get(i).tick > displayStrings.get(i).maxTick) {
				displayStrings.remove(i);
			}
		}

		// Reset values, and add to destroy tick
		for (int x = 0; x < s; x++) {
			for (int y = 0; y < s; y++) {
				offSet[x][y] = new Point(0, 0);
			}
		}

		if (destructionTick != -1) {
			destructionTick--;
			if (destructionTick == 0) {
				destructionTick--;
				// Explode all the pieces & set newBonus = 0
				newBonus = 0;
				for (int x = 0; x < s; x++) {
					for (int y = 0; y < s; y++) {
						if (grid[x][y] == bonusPiece) {
							removePiece(x, y);
						}
					}
				}
			}
		}

		if (explosionTick != -1) {
			explosionTick++;
			if (explosionTick > 36) {
				explosionTick = -1;
				explosionLocation.clear();
			}
		}

		if (destroyTick != -1) {
			destroyTick++;
		}

		if (destroyTick > maxDestroyTick) {
			for (int x = 0; x < s; x++) {
				for (int y = 0; y < s; y++) {
					if (destroying[x][y]) {
						frameNum = 0;
						gamePlayable = false;
						destroyTick = -1;
						destroying[x][y] = false;
						grid[x][y] = 0;
					}
				}
			}
		}

		if (Listening.mouseDown) {
			if (getMousePosition() != null && gamePlayable) {
				if (Listening.gridPressLocation.x >= 0 && Listening.gridPressLocation.y >= 0 && Listening.gridPressLocation.x < s && Listening.gridPressLocation.y < s) {
					offSetGrid(Listening.gridPressLocation, new Point(getMousePosition().x / tileSize, getMousePosition().y / tileSize), Listening.pressLocation, getMousePosition());
				}
			}
		}

		totalTicks++;
		if (totalTicks % (ticksPerSecond * 2) == 0) {
			// saveStats();
		}

		if (gamePlayable) {
			for (int i = 0; i < lastColors.length; i++) {
				lastColors[i] = 0;
			}

		} else if (destroyTick == -1) {
			// Pieces dropping:
			frameNum++;
			if (frameNum == maxFrame) {
				frameNum = 0;
				boolean good = true;
				for (int x = 0; x < s; x++) {
					for (int y = s - 1; y >= 0; y--) {
						dropping[x][y] = grid[x][y] == 0;
						if (grid[x][y] == 0) {
							good = false;
							if (y == 0) {
								// New piece:
								newBall(x, y);

							} else {
								grid[x][y] = grid[x][y - 1];
								grid[x][y - 1] = 0;
							}
						}
					}
				}
				if (good) {
					// (Has to be a new combo)
					updateBoard();
				}
			}

		}
	}

	public void saveStats() {
		try {
			File f = new File("stats.txt");
			if (!f.exists()) {
				f.createNewFile();
			}
			PrintWriter out = new PrintWriter(new FileWriter("stats.txt", true));
			out.println((int) score);
			out.close();
		} catch (Exception e) {

		}
	}

	public static void updateBoard() {
		boolean updated = false;
		addCombo();
		numCleared = 0;
		gamePlayable = true;
		if (destroyTick == 0) {
			numCleared++;
			gamePlayable = false;
			frameNum = 0;
			return;
		}
		while (checkForClear()) {
			numCleared++;
			gamePlayable = false;
			frameNum = 0;
			updated = true;
		}
		if (!updated) {
			numCombo = 0;
		} else {
			comboMessage();
		}
	}

	public void newBall(int x, int y) {
		grid[x][y] = new Random().nextInt(colors.length - 1) + 1;

		// Wild piece:
		if (new Random().nextInt(50) == 0 || newBonus > 0 && new Random().nextInt(25) == 0) {
			grid[x][y] = explosivePiece;
		}

		int n = new Random().nextInt((int) (lastColors.length * 1.5));
		if (n < lastColors.length) {
			if (lastColors[n] != 0) {
				grid[x][y] = lastColors[n];
			}
		}

		if (numSame > 0 && lastColors[lastColors.length - 1] != 0 && Math.random() < 0.4) {
			numSame--;
			grid[x][y] = lastColors[lastColors.length - 1];
		}

		// Goes to the "Last colors" to be reused only if a color:
		if (grid[x][y] < colors.length) {
			for (int i = 0; i < lastColors.length - 1; i++) {
				lastColors[i] = lastColors[i + 1];
			}
			lastColors[lastColors.length - 1] = grid[x][y];
		}

		// BONUS PIECES:
		if (newBonus > 0) {
			if (Math.random() * 200 < newBonus + 40 - numBonus) {
				newBonus--;
				numBonus++;
				grid[x][y] = bonusPiece;
			}
		}
	}

	public static void checkForBonus(int c) {
		for (int x = 0; x < s; x++) {
			for (int y = 0; y < s; y++) {
				if (grid[x][y] == c && !destroying[x][y]) {
					return; // No bonus.
				}
			}
		}
		// Bonus occured:
		score += CLEAR_ALL;
		displayStrings.add(new DisplayStrings(CLEAR_ALL, tileSize * s / 2, colors[c], "All Cleared!"));

		if (newBonus == 0) {
			newBonus += 16;
			destructionTick += DESTRUCTION_TIMER;
		}
	}

	public void offSetGrid(Point a, Point b, Point c, Point d) {
		Point startingLocation = new Point(0, 0);
		double rotation = Math.atan2(a.x * tileSize + tileSize / 2 - d.x, a.y * tileSize + tileSize / 2 - d.y);
		int distance = 0;
		int[] values = new int[s];
		if (rotation < -Math.PI / 8 * 7 || rotation > Math.PI / 8 * 7) {
			direction = new Point(0, -1);
			startingLocation = new Point(a.x, s - 1);
		} else if (rotation < -Math.PI / 8 * 5) {
			direction = new Point(-1, -1);
			startingLocation = new Point(a.x + Math.min((s - 1) - a.x, (s - 1) - a.y), a.y + Math.min((s - 1) - a.x, (s - 1) - a.y));
			values = new int[s - (Math.abs(a.x - a.y))];
		} else if (rotation < -Math.PI / 8 * 3) {
			direction = new Point(-1, 0);
			startingLocation = new Point(s - 1, a.y);
		} else if (rotation < -Math.PI / 8) {
			direction = new Point(-1, 1);
			startingLocation = new Point(Math.min(a.x + a.y, s - 1), Math.max(a.x + a.y - (s - 1), 0));
			values = new int[s - (Math.abs((a.x + a.y) - (s - 1)))];
		} else if (rotation < Math.PI / 8) {
			direction = new Point(0, 1);
			startingLocation = new Point(a.x, 0);
		} else if (rotation < Math.PI / 8 * 3) {
			direction = new Point(1, 1);
			startingLocation = new Point(a.x - Math.min(a.x, a.y), a.y - Math.min(a.x, a.y));
			values = new int[s - (Math.abs(a.x - a.y))];
		} else if (rotation < Math.PI / 8 * 5) {
			direction = new Point(1, 0);
			startingLocation = new Point(0, a.y);
		} else {
			direction = new Point(1, -1);
			startingLocation = new Point(Math.max(a.x + a.y - (s - 1), 0), Math.min(a.x + a.y, s - 1));
			values = new int[s - (Math.abs((a.x + a.y) - (s - 1)))];
		}
		distance = createDistance(direction.x != 0 && direction.y != 0, a, d);

		movingLength = s - values.length;

		boolean safe = false;
		for (int i = 0; i < values.length; i++) {
			values[i] = grid[startingLocation.x + direction.x * i][startingLocation.y + direction.y * i];

			// Bonus pieces are unmovable:
			if (values[i] == explosivePiece) {
				safe = true;
			}

		}

		// Horizontal / Vertical maximum movement
		if (!safe && distance > tileSize / 8 && (direction.x == 0 || direction.y == 0)) {
			for (int i = 0; i < values.length; i++) {
				if (values[i] == bonusPiece) {
					distance = tileSize / 8;
				}
			}
		}

		// Diagonal maximum movement:
		if (!safe && distance > tileSize / 4 && (direction.x != 0 && direction.y != 0)) {
			for (int i = 0; i < values.length; i++) {
				if (values[i] == bonusPiece) {
					distance = (int) (tileSize / 8 * 1.3);
				}
			}
		}

		// Set values
		for (int i = 0; i < values.length; i++) {
			if (safe || values[i] != bonusPiece) {
				offSet[startingLocation.x + direction.x * i][startingLocation.y + direction.y * i] = new Point(direction.x * distance, direction.y * distance);
			}
		}
	}

	public int createDistance(boolean diagonal, Point a, Point d) {
		if (diagonal) {
			return (int) (Math.sqrt(Math.pow(a.x * tileSize + tileSize / 2 - d.x, 2) + Math.pow(a.y * tileSize + tileSize / 2 - d.y, 2)) / Math.sqrt(2) + 0.5);
		} else {
			return (int) (Math.sqrt(Math.pow(a.x * tileSize + tileSize / 2 - d.x, 2) + Math.pow(a.y * tileSize + tileSize / 2 - d.y, 2)) + 0.5);
		}
	}

	public static void addCombo() {
		if (numCombo < MAX_COMBO) {
			numCombo++;
		}

	}

	public static void comboMessage() {
		for (int i = 0; i < displayStrings.size(); i++) {
			if (displayStrings.get(i).string.charAt(0) == 'x') {
				displayStrings.remove(i);
				i--;
			}
		}
		displayStrings.add(new DisplayStrings(numCombo));
	}

	public static boolean clearBonus() {
		int y = s - 1;
		for (int x = 0; x < s; x++) {
			if (grid[x][y] == bonusPiece && !destroying[x][y]) {

				// Clear it
				score += CLEAR_BONUS_PIECE;

				displayStrings.add(new DisplayStrings(CLEAR_BONUS_PIECE, x * tileSize));

				removePiece(x, y);

				// Bonus effect: 
				numSame += 2;

				return true;
			} else if (grid[x][y] == explosivePiece && !destroying[x][y]) {
				// Explodes explosive piece
				explode(x, y);
				return true;
			}
		}

		return false;
	}

	public static void removePieces(ArrayList<Integer> x, ArrayList<Integer> y) {
		for (int i = 0; i < x.toArray().length; i++) {
			removePiece(x.get(i), y.get(i));
		}
	}

	public static void explode(int xL, int yL) {
		int color = new Random().nextInt(colors.length - 1) + 1;
		int scoreAmount = (int) (2 * (Math.pow(numCombo, 2.0)) * 10000);
		score += scoreAmount;
		displayStrings.add(new DisplayStrings(scoreAmount, xL * tileSize));

		removePieceDirectly(xL, yL);
		for (int x = xL - 1; x <= xL + 1; x++) {
			for (int y = yL - 1; y <= yL + 1; y++) {
				if (x >= 0 && y >= 0 && x < s && y < s) {
					if (grid[x][y] == explosivePiece && !destroying[x][y] && (x != xL || y != yL)) {
						explode(x, y);
					} else if (x != xL || y != yL) {
						removePiece(x, y);
					}
				}
			}
		}
		explosionLocation.add(new Point(xL, yL));
		explosionTick = 0;
	}

	public static void removePieceDirectly(int x, int y) {
		destroying[x][y] = true;
		destroyTick = 0;
	}

	public static void removePiece(int x, int y) {
		if (grid[x][y] == explosivePiece && !destroying[x][y]) {
			explode(x, y);
		} else {
			if (grid[x][y] == bonusPiece) {
				numBonus--;
				// Check to see if all bonus peices have been cleared. (ONLY if IN destroying phase)
				if (newBonus == 0 && grid[x][y] == bonusPiece && destructionTick != -1) {
					boolean good = true;
					for (int xL = 0; xL < s; xL++) {
						for (int yL = 0; yL < s; yL++) {
							if (grid[xL][yL] == bonusPiece && !destroying[xL][yL] && !(x == xL && y == yL)) {
								good = false;
							}
						}
					}
					if (good) {
						score += CLEAR_ALL_BONUS;
						displayStrings.add(new DisplayStrings(CLEAR_ALL_BONUS, tileSize * s / 2, new Color(220, 220, 220), "All blockers cleared!"));
						destructionTick = -1;
					}
				}
			}

			removePieceDirectly(x, y);
		}
	}

	public static boolean checkForClear() {

		if (clearBonus()) {
			return true;
		}

		int totalInCombination = 0;
		ArrayList<Integer> locX = new ArrayList<Integer>();
		ArrayList<Integer> locY = new ArrayList<Integer>();
		boolean used[][] = new boolean[s][s];
		boolean doneWith[][] = new boolean[s][s];
		int colorClearing;

		for (int x = 0; x < s; x++) {
			for (int y = 0; y < s; y++) {
				used[x][y] = false;
				doneWith[x][y] = false;
			}
		}

		for (int x = 0; x < s; x++) {
			for (int y = 0; y < s; y++) {
				if (!used[x][y] && grid[x][y] != 0 && grid[x][y] < colors.length && !destroying[x][y]) {
					locX.clear();
					locY.clear();
					locX.add(x);
					locY.add(y);
					used[x][y] = true;
					totalInCombination = 1;
					colorClearing = grid[x][y];
					for (int v = 0; v < locX.toArray().length; v++) {
						if (!doneWith[locX.get(v)][locY.get(v)]) {
							doneWith[locX.get(v)][locY.get(v)] = true;

							Point[] sides = { new Point(0, -1), new Point(-1, 0), new Point(0, 1), new Point(1, 0) };

							for (int i = 0; i < sides.length; i++) {
								if (nextTo(colorClearing, locX.get(v), locY.get(v), locX.get(v) + sides[i].x, locY.get(v) + sides[i].y)) {
									if (!used[locX.get(v) + sides[i].x][locY.get(v) + sides[i].y] && !destroying[locX.get(v) + sides[i].x][locY.get(v) + sides[i].y]) {
										used[locX.get(v) + sides[i].x][locY.get(v) + sides[i].y] = true;
										locX.add(locX.get(v) + sides[i].x);
										locY.add(locY.get(v) + sides[i].y);
										totalInCombination++;
									}
								}
							}
						}
					}

					// Save the values to the cubes:
					for (int v = 0; v < locX.toArray().length; v++) {
						totalInCombo[locX.get(v)][locY.get(v)] = totalInCombination;
					}

					if (totalInCombination >= COMBO_REQUIREMENT) {
						boolean good = false;
						for (int v = 0; v < locX.toArray().length; v++) {
							if (locY.get(v) == s - 1) {
								good = true;
								break;
							}
						}

						if (good/* || totalInCombination >= 5 */) {

							// Set the destroying sequence

							int scoreAmount = (int) (Math.pow((double) totalInCombination, 1.5) * (Math.pow(numCombo, 2.0)) * 10000);

							score += scoreAmount;

							displayStrings.add(new DisplayStrings(scoreAmount, locX.get(new Random().nextInt(locX.toArray().length)) * tileSize));

							removePieces(locX, locY);

							if (totalInCombination >= COMBO_REQUIREMENT_BONUS) {
								checkForBonus(colorClearing);
							}

							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public static boolean nextTo(int c, int x, int y, int nX, int nY) {
		if (nY > y && nY < s) {
			if (grid[nX][nY] == c) {
				return true;
			}
		} else if (nY < y && nY >= 0) {
			if (grid[nX][nY] == c) {
				return true;
			}
		} else if (nX > x && nX < s) {
			if (grid[nX][nY] == c) {
				return true;
			}
		} else if (nX < x && nX >= 0) {
			if (grid[nX][nY] == c) {
				return true;
			}
		}
		return false;
	}

	public void drawCircle(int type, int x, int y, int width, int height, float alpha, Graphics2D g) {
		g.setColor(Color.black);

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

		if (type == bonusPiece) {
			g.drawImage(bonusPieceImage, x, y, width, height, null);
			if (destructionTick < SHAKE_TICK && destructionTick != -1) {
				g.drawImage(bonusPieceImage, x + (int) (Math.random() * 4) - 2, y + (int) (Math.random() * 4) - 2, width, height, null);
			}
		} else if (type == explosivePiece) {
			g.drawImage(explosivePieceImage, x, y, width, height, null);
		} else {
			g.drawImage(circlesImage, x, y, x + width, y + height, (type - 1) * tileSize, 0, type * tileSize, tileSize, null);
		}

	}

	public void drawCircles(boolean isOffSet, Graphics2D g2) {
		for (int x = 0; x < s; x++) {
			for (int y = 0; y < s; y++) {
				if (grid[x][y] != 0 && ((offSet[x][y].x != 0 || offSet[x][y].y != 0) == isOffSet)) {
					if (dropping[x][y]) {
						drawCircle(grid[x][y], x * tileSize, y * tileSize - tileSize + frameNum * tileSize / maxFrame, tileSize, tileSize, 1, g2);
					} else {
						if (x * tileSize - offSet[x][y].x + tileSize / 2 >= 0 && x * tileSize - offSet[x][y].x + tileSize / 2 <= tileSize * s && y * tileSize - offSet[x][y].y + tileSize / 2 >= 0
								&& y * tileSize - offSet[x][y].y + tileSize / 2 <= tileSize * s) { // diagonal
							if (destroyTick == -1 || !destroying[x][y]) {
								drawCircle(grid[x][y], (x * tileSize - offSet[x][y].x + tileSize / 2 + tileSize * s * 2) % (tileSize * s) - tileSize / 2,
										(y * tileSize - offSet[x][y].y + tileSize / 2 + tileSize * s * 2) % (tileSize * s) - tileSize / 2, tileSize, tileSize, 1, g2);
							} else {
								drawCircle(grid[x][y], (x * tileSize - offSet[x][y].x + tileSize / 2 + tileSize * s * 2) % (tileSize * s) - tileSize / 2,
										(y * tileSize - offSet[x][y].y + tileSize / 2 + tileSize * s * 2) % (tileSize * s) - tileSize / 2, tileSize, tileSize,
										1 - ((float) destroyTick / maxDestroyTick), g2);
							}
						} else {
							drawCircle(grid[x][y], ((x - direction.x * movingLength) * tileSize - offSet[x][y].x + tileSize / 2 + tileSize * s * 2) % (tileSize * s) - tileSize / 2, ((y - direction.y
									* movingLength)
									* tileSize - offSet[x][y].y + tileSize / 2 + tileSize * s * 2)
									% (tileSize * s) - tileSize / 2, tileSize, tileSize, 1, g2);
						}
					}
				}
			}
		}
	}

	public void render() {
		((VolatileImage) screen).validate(getGraphicsConfiguration());
		Graphics g = screen.getGraphics();
		Graphics2D g2 = (Graphics2D) g;
		g.setColor(new Color(0, 100, 175));
		g.fillRect(0, 0, size.width, size.height);
		mousePos = getMousePosition();

		g.drawImage(backGround, 0, 0, 700, 700, null);

		g.setFont(new Font("Verdana", Font.PLAIN, 60));
		g.setColor(Color.BLACK);
		//g.drawString(String.valueOf((int) score / 1000), 10, 753);
		//g.fillRect(0, 700, (int) ((score / 10000000.0) * 700), 55);
		g.drawImage(bgScoreImage, 0, 700, 700, 50, null);
		g.drawImage(fgScoreImage, 0, 700, (int) ((fakeScore / 100000000.0) * 700), 750, 700 - (int) ((fakeScore / 100000000.0) * 700), 0, 700, 50, null);

		int strokeSize = 2;

		g2.setStroke(new BasicStroke(strokeSize));

		// Draw containing squares
		for (int x = 0; x < s; x++) {
			for (int y = 0; y < s; y++) {
				if (!dropping[x][y] && grid[x][y] < colors.length && grid[x][y] > 0 && offSet[x][y].x == 0 && offSet[x][y].y == 0 && totalInCombo[x][y] >= COMBO_REQUIREMENT) {

					if (grid[x][y] == bonusPiece) {
						g.setColor(new Color(40, 40, 40));
					} else if (grid[x][y] == explosivePiece) {
						g.setColor(new Color(200, 200, 200));
					} else {
						g.setColor(new Color(colors[grid[x][y]].getRed() - 25, colors[grid[x][y]].getGreen() - 25, colors[grid[x][y]].getBlue() - 25));
					}
					g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
					g.setColor(Color.black);
					g.drawRect(x * tileSize, y * tileSize, tileSize, tileSize);
				}
			}
		}

		// Draw circles with the moving circles in front:
		drawCircles(false, g2);
		drawCircles(true, g2);

		if (explosionTick != -1) {

			for (int i = 0; i < explosionLocation.toArray().length; i++) {
				g.drawImage(explosionAnimation, explosionLocation.get(i).x * tileSize - tileSize, explosionLocation.get(i).y * tileSize - tileSize, explosionLocation.get(i).x * tileSize + tileSize
						* 2, explosionLocation.get(i).y * tileSize + tileSize * 2, explosionTick / 3 * 134, 0, (explosionTick / 3 + 1) * 134, 134, null);
			}

		}

		for (int i = 0; i < displayStrings.toArray().length; i++) {
			displayStrings.get(i).render(g2);
		}

		g = getGraphics();

		g.drawImage(screen, 0, 0, size.width, size.height, 0, 0, size.width, size.height, null);
		g.dispose();
	}

	public void run() {
		screen = createVolatileImage(size.width, size.height);
		long lastTime = System.nanoTime();
		double unprocessed = 0;
		double nsPerTick = 1000000000.0 / (double) ticksPerSecond;
		while (isRunning) {
			long now = System.nanoTime();
			unprocessed += (now - lastTime) / nsPerTick;
			lastTime = now;
			while (unprocessed >= 1) {
				tick();
				render(); // Only needs to render the tick has changed, as 60 frames / second should be negliblely small for stuff that suggests this should render.
				unprocessed -= 1;
			}
			{
				if (unprocessed < 1) {
					try {
						Thread.sleep((int) ((1 - unprocessed) * nsPerTick) / 1000000, (int) ((1 - unprocessed) * nsPerTick) % 1000000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

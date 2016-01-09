import java.awt.*;
import java.util.*;

public class DisplayStrings {
	String string;
	int maxTick;
	int tick;
	int size;
	int x;
	int y;
	Color color;

	public DisplayStrings(int score, int xLocation) {
		string = pickString(score);
		tick = 0;
		size = (determineValue(score)) + 5;
		maxTick = size + 10;

		// Location is placed.
		x = xLocation + new Random().nextInt(Component.tileSize * 2) - Component.tileSize * 1;
		if (x < Component.tileSize / 4) {
			x = new Random().nextInt(Component.tileSize) + Component.tileSize / 4;
		} else if (x > Component.tileSize * Component.s - Component.tileSize / 4) {
			x = Component.tileSize * Component.s - new Random().nextInt(Component.tileSize) - Component.tileSize / 4;
		}
		y = Component.tileSize * 3 + new Random().nextInt(Component.tileSize * 3);
		color = new Color(220, 220, 220);
	}

	public DisplayStrings(int score, int xLocation, Color c, String s) {
		string = s;
		tick = 0;
		size = (determineValue(score)) + 5;
		maxTick = size + 10;
		// Special case scenario's:
		if (string.equals("All Cleared!")) {
			maxTick += 40;
		} else if (string.equals("All blockers cleared!")) {
			maxTick += 20;
		}
		x = xLocation + new Random().nextInt(Component.tileSize * 2) - Component.tileSize * 1;
		if (x < Component.tileSize / 4) {
			x = new Random().nextInt(Component.tileSize) + Component.tileSize / 4;
		} else if (x > Component.tileSize * Component.s - Component.tileSize / 4) {
			x = Component.tileSize * Component.s - new Random().nextInt(Component.tileSize) - Component.tileSize / 4;
		}
		y = Component.tileSize * 3 + new Random().nextInt(Component.tileSize * 3);
		color = c;
	}

	public DisplayStrings(int combo) {
		string = "x" + combo;
		tick = 0;
		size = 40;
		maxTick = 30 * combo;
		// ESTIMATION TACTICS
		x = Component.width / 2 - 10;
		y = Component.height - 8;
		color = new Color(0, 0, 0);
	}

	public String pickString(int score) {
		if (score < 0) {
			return "";
		} else if (score < 500000) {
			int n = new Random().nextInt(4); // Set this number to the number of messages
			String strings[] = { "Good!", "Nice!", "Fine!", "Clear!" };
			return strings[n];
		} else if (score < 5000000) {
			int n = new Random().nextInt(4); // Set this number to the number of messages
			String strings[] = { "Excellent!", "Great!", "Amazing!", "Awesome!" };
			return strings[n];
		} else if (score < 50000000) {
			int n = new Random().nextInt(2); // Set this number to the number of messages
			String strings[] = { "Incredible!", "Unbelievable!" };
			return strings[n];
		} else {
			return "~Unbeatable!";
		}
	}

	public int determineValue(int score) {
		for (double i = 0; true; i += 0.5) {
			if (score < Math.pow(3, i)) {
				return (int) (i * 3);
			}
		}
	}

	public void tick() {
		tick++;
	}

	public void render(Graphics2D g2) {
		g2.setFont(new Font("Impact", Font.PLAIN, size));
		int alpha;
		if (tick >= maxTick / 4.0 && tick <= maxTick / 4.0 * 3) {
			alpha = 255;
		} else if (tick < maxTick / 4.0) {
			alpha = (int) (tick / ((maxTick) / 4.0) * 255);
		} else {
			alpha = (int) ((maxTick - tick) / ((maxTick) / 4.0) * 255);
		}

		g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));

		// Positioning methodology
		int width = (int) (g2.getFontMetrics().getStringBounds(string, g2).getWidth());
		int x = this.x - width / 2; // (Centered at this position) x = CENTER
		if (this.x - width / 2 < Component.tileSize) {
			x += width / 2; // x = LEFT
		} else if (this.x + width / 2 > Component.tileSize * Component.s - Component.tileSize) {
			x -= width / 2; // x = RIGHT
		}
		if (string.equals("All Cleared!")) {
			g2.drawImage(Component.allClearedImage, x - 18, y - size - 2, null);
		}
		if (string.equals("All blockers cleared!")) {
			g2.drawImage(Component.allBlockersClearedImage, x, y, null);
		} else {
			g2.drawString(string, x, y);
		}
	}
}

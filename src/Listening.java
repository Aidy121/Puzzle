import java.awt.*;
import java.awt.event.*;

public class Listening implements MouseListener, MouseWheelListener, KeyListener {
	public static Point pressLocation = new Point(-1, -1); //off screen = none\
	public static Point gridPressLocation = new Point(-1, -1);
	public static Point currLocation = new Point(-1, -1); //off screen = none
	public static Point lastLocation = new Point(-1, -1); //off screen = none
	public static boolean mouseDown = false;

	public void mouseClicked(MouseEvent e) {

	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			mouseDown = true;
			pressLocation = new Point(e.getX(), e.getY()); //nothing happens
			gridPressLocation = new Point(pressLocation.x / Component.tileSize, pressLocation.y / Component.tileSize);
			lastLocation = pressLocation;
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			mouseDown = false;
		}
	}

	public void boardMove(Point a, Point b) {
		Point startingLocation = new Point(0, 0);
		Point direction = new Point(0, 0);
		double rotation = Math.atan2(a.x - b.x, a.y - b.y);
		int distance = 0;
		int[] values = new int[Component.s];

		if (rotation < -Math.PI / 8 * 7 || rotation > Math.PI / 8 * 7) {
			direction = new Point(0, -1);
			startingLocation = new Point(a.x, Component.s - 1);
		} else if (rotation < -Math.PI / 8 * 5) {
			direction = new Point(-1, -1);
			startingLocation = new Point(a.x + Math.min((Component.s - 1) - a.x, (Component.s - 1) - a.y), a.y + Math.min((Component.s - 1) - a.x, (Component.s - 1) - a.y));
			values = new int[Component.s - (Math.abs(a.x - a.y))];
		} else if (rotation < -Math.PI / 8 * 3) {
			direction = new Point(-1, 0);
			startingLocation = new Point(Component.s - 1, a.y);
		} else if (rotation < -Math.PI / 8) {
			direction = new Point(-1, 1);
			startingLocation = new Point(Math.min(a.x + a.y, Component.s - 1), Math.max(a.x + a.y - (Component.s - 1), 0));
			values = new int[Component.s - (Math.abs((a.x + a.y) - (Component.s - 1)))];
		} else if (rotation < Math.PI / 8) {
			direction = new Point(0, 1);
			startingLocation = new Point(a.x, 0);
		} else if (rotation < Math.PI / 8 * 3) {
			direction = new Point(1, 1);
			startingLocation = new Point(a.x - Math.min(a.x, a.y), a.y - Math.min(a.x, a.y));
			values = new int[Component.s - (Math.abs(a.x - a.y))];
		} else if (rotation < Math.PI / 8 * 5) {
			direction = new Point(1, 0);
			startingLocation = new Point(0, a.y);
		} else {
			direction = new Point(1, -1);
			startingLocation = new Point(Math.max(a.x + a.y - (Component.s - 1), 0), Math.min(a.x + a.y, Component.s - 1));
			values = new int[Component.s - (Math.abs((a.x + a.y) - (Component.s - 1)))];
		}
		distance = createDistance(direction.x != 0 && direction.y != 0, a, b);

		if (distance != 0) { // (has to be moved)

			boolean exploding = false;
			for (int i = 0; i < values.length; i++) {
				values[i] = Component.grid[startingLocation.x + direction.x * i][startingLocation.y + direction.y * i];

				// Bonus pieces are unmovable:
				if (values[i] == Component.explosivePiece) {
					exploding = true;
				}

			}

			if (!exploding) {
				for (int i = 0; i < values.length; i++) {
					if (values[i] == Component.bonusPiece) {
						return;
					}
				}
			}

			for (int i = 0; i < values.length; i++) {
				Component.grid[startingLocation.x + direction.x * i][startingLocation.y + direction.y * i] = values[(i + distance) % values.length];

			}

			// Reset all the offsets:
			for (int x = 0; x < Component.s; x++) {
				for (int y = 0; y < Component.s; y++) {
					Component.offSet[x][y] = new Point(0, 0);
				}
			}

			// Explosive pieces
			if (exploding) {
				for (int i = 0; i < values.length; i++) {
					if (values[(i + distance) % values.length] == Component.explosivePiece) {

						Component.explode(startingLocation.x + direction.x * i, startingLocation.y + direction.y * i);

					}
				}
			}

			// Update the board & score:
			// Component.score = (int) (Component.score / 1.05);

			Component.updateBoard();
		}

	}

	public int createDistance(boolean diagonal, Point a, Point b) {
		if (diagonal) {
			return (int) (Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2)) / Math.sqrt(2) + 0.5);
		} else {
			return (int) (Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2)) + 0.5);
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (Component.gamePlayable && e.getButton() == MouseEvent.BUTTON1 && mouseDown) {
			if (gridPressLocation.x >= 0 && gridPressLocation.y >= 0 && gridPressLocation.x < Component.s && gridPressLocation.y < Component.s && e.getX() / Component.tileSize >= 0
					&& e.getY() / Component.tileSize >= 0 && e.getX() / Component.tileSize < Component.s && e.getY() / Component.tileSize < Component.s) {
				boardMove(gridPressLocation, new Point(e.getX() / Component.tileSize, e.getY() / Component.tileSize));
			}

		}
		mouseDown = false;
		pressLocation = new Point(-1, -1);
		gridPressLocation = new Point(-1, -1);
	}

	public void mouseWheelMoved(MouseWheelEvent e) {

	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}
}

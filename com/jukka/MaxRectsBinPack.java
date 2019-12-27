package com.jukka;

import java.util.Vector;

/**
 * @file MaxRectsBinPack.h
 * @author Jukka Jylänki
 * 
 * @brief Implements different bin packer algorithms that use the MAXRECTS data
 *        structure.
 * 
 *        This work is released to Public Domain, do whatever you want with it.
 * 
 *        MaxRectsBinPack implements the MAXRECTS data structure and different
 *        bin packing algorithms that use this structure.
 * 
 */
public class MaxRectsBinPack {
	/// Specifies the different heuristic rules that can be used when deciding where
	/// to place a new rectangle.
	public enum FreeRectChoiceHeuristic {
		RectBestShortSideFit, /// < -BSSF: Positions the rectangle against the short side of a free rectangle
								/// into which it fits the best.
		RectBestLongSideFit, /// < -BLSF: Positions the rectangle against the long side of a free rectangle
								/// into which it fits the best.
		RectBestAreaFit, /// < -BAF: Positions the rectangle into the smallest free rect into which it
							/// fits.
		RectBottomLeftRule, /// < -BL: Does the Tetris placement.
		RectContactPointRule, /// < -CP: Choosest the placement where the rectangle touches other rects as
								/// much as possible.
		RectBestSquareFit
	};

	int binWidth;
	int binHeight;

	boolean binAllowFlip;

	Vector<Rect> usedRectangles;
	Vector<Rect> freeRectangles;

	public MaxRectsBinPack() {
		binWidth = 0;
		binHeight = 0;
	}

	/// Instantiates a bin of the given size.
	/// @param allowFlip Specifies whether the packing algorithm is allowed to
	/// rotate the input rectangles by 90 degrees to consider a better placement.
	public MaxRectsBinPack(int width, int height, boolean allowFlip) {
		Init(width, height, allowFlip);
	}

	/// (Re)initializes the packer to an empty bin of width x height units. Call
	/// whenever
	/// you need to restart with a new bin.
	public void Init(int width, int height, boolean allowFlip) {
		binAllowFlip = allowFlip;
		binWidth = width;
		binHeight = height;

		Rect n = new Rect();
		n.x = 0;
		n.y = 0;
		n.width = width;
		n.height = height;
		usedRectangles = new Vector<>();
		freeRectangles = new Vector<>();

		usedRectangles.clear();

		freeRectangles.clear();
		freeRectangles.add(n);
	}

	/// Inserts a single rectangle into the bin, possibly rotated.
	public Rect Insert(int width, int height, FreeRectChoiceHeuristic method) {
		Rect newNode = new Rect();
		// Unused in this function. We don't need to know the score after finding the
		// position.
		int score1 = Integer.MAX_VALUE;
		int score2 = Integer.MAX_VALUE;
		switch (method) {
		case RectBestShortSideFit:
			newNode = FindPositionForNewNodeBestShortSideFit(width, height, score1, score2);
			break;
		case RectBottomLeftRule:
			newNode = FindPositionForNewNodeBottomLeft(width, height, score1, score2);
			break;
		case RectContactPointRule:
			newNode = FindPositionForNewNodeContactPoint(width, height, score1);
			break;
		case RectBestLongSideFit:
			newNode = FindPositionForNewNodeBestLongSideFit(width, height, score2, score1);
			break;
		case RectBestAreaFit:
			newNode = FindPositionForNewNodeBestAreaFit(width, height, score1, score2);
			break;
		case RectBestSquareFit:
			newNode = FindPositionForNewNodeBestSquareFit(width, height, score1, score2);
			break;
		}

		if (newNode.height == 0)
			return newNode;

		int numRectanglesToProcess = freeRectangles.size();
		for (int i = 0; i < numRectanglesToProcess; ++i) {
			if (SplitFreeNode(freeRectangles.get(i), newNode)) {
				freeRectangles.remove(i);
				--i;
				--numRectanglesToProcess;
			}
		}

		PruneFreeList();

		usedRectangles.add(newNode);
		return newNode;
	}

	public void Insert(Vector<RectSize> rects, Vector<Rect> dst, FreeRectChoiceHeuristic method) {
		dst.clear();

		while (rects.size() > 0) {
			int bestScore1 = Integer.MAX_VALUE;
			;
			int bestScore2 = Integer.MAX_VALUE;
			;
			int bestRectIndex = -1;
			Rect bestNode = new Rect();

			for (int i = 0; i < rects.size(); ++i) {
				int score1 = 0;
				int score2 = 0;
				Rect newNode = ScoreRect(rects.get(i).width, rects.get(i).height, method, score1, score2);

				if (score1 < bestScore1 || (score1 == bestScore1 && score2 < bestScore2)) {
					bestScore1 = score1;
					bestScore2 = score2;
					bestNode = newNode;
					bestRectIndex = i;
				}
			}

			if (bestRectIndex == -1)
				return;

			PlaceRect(bestNode);
			dst.add(bestNode);
			rects.remove(bestRectIndex);
		}
	}

	/// Places the given rectangle into the bin.
	void PlaceRect(Rect node) {
		int numRectanglesToProcess = freeRectangles.size();
		for (int i = 0; i < numRectanglesToProcess; ++i) {
			if (SplitFreeNode(freeRectangles.get(i), node)) {
				freeRectangles.remove(i);
				--i;
				--numRectanglesToProcess;
			}
		}

		PruneFreeList();

		usedRectangles.add(node);
	}

	/// Computes the placement score for placing the given rectangle with the given
	/// method.
	/// @param score1 [out] The primary placement score will be outputted here.
	/// @param score2 [out] The secondary placement score will be outputted here.
	/// This isu sed to break ties.
	/// @return This struct identifies where the rectangle would be placed if it
	/// were placed.
	Rect ScoreRect(int width, int height, FreeRectChoiceHeuristic method, int score1, int score2) {
		Rect newNode = new Rect();
		score1 = Integer.MAX_VALUE;
		score2 = Integer.MAX_VALUE;
		switch (method) {
		case RectBestShortSideFit:
			newNode = FindPositionForNewNodeBestShortSideFit(width, height, score1, score2);
			break;
		case RectBottomLeftRule:
			newNode = FindPositionForNewNodeBottomLeft(width, height, score1, score2);
			break;
		case RectContactPointRule:
			newNode = FindPositionForNewNodeContactPoint(width, height, score1);
			score1 = -score1; // Reverse since we are minimizing, but for contact point score bigger is
								// better.
			break;
		case RectBestLongSideFit:
			newNode = FindPositionForNewNodeBestLongSideFit(width, height, score2, score1);
			break;
		case RectBestAreaFit:
			newNode = FindPositionForNewNodeBestAreaFit(width, height, score1, score2);
			break;
		case RectBestSquareFit:
			newNode = FindPositionForNewNodeBestSquareFit(width, height, score1, score2);
			break;
		default:
			break;
		}

		// Cannot fit the current rectangle.
		if (newNode.height == 0) {
			score1 = Integer.MAX_VALUE;
			score2 = Integer.MAX_VALUE;
		}

		return newNode;

	}

	/// Computes the ratio of used surface area to the total bin area.
	public float Occupancy() {
		long usedSurfaceArea = 0;
		for (int i = 0; i < usedRectangles.size(); ++i)
			usedSurfaceArea += usedRectangles.get(i).width * usedRectangles.get(i).height;

		return (float) usedSurfaceArea / (binWidth * binHeight);

	}

	private Rect FindPositionForNewNodeBottomLeft(int width, int height, int bestY, int bestX) {
		Rect bestNode = new Rect();

		bestY = Integer.MAX_VALUE;
		bestX = Integer.MAX_VALUE;

		for (int i = 0; i < freeRectangles.size(); ++i) {
			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width >= width && freeRectangles.get(i).height >= height) {
				int topSideY = freeRectangles.get(i).y + height;
				if (topSideY < bestY || (topSideY == bestY && freeRectangles.get(i).x < bestX)) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = width;
					bestNode.height = height;
					bestY = topSideY;
					bestX = freeRectangles.get(i).x;
				}
			}
			if (binAllowFlip && freeRectangles.get(i).width >= height && freeRectangles.get(i).height >= width) {
				int topSideY = freeRectangles.get(i).y + width;
				if (topSideY < bestY || (topSideY == bestY && freeRectangles.get(i).x < bestX)) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = height;
					bestNode.height = width;
					bestY = topSideY;
					bestX = freeRectangles.get(i).x;
				}
			}
		}
		return bestNode;
	}

	private Rect FindPositionForNewNodeBestShortSideFit(int width, int height, int bestShortSideFit,
			int bestLongSideFit) {
		Rect bestNode = new Rect();
		bestShortSideFit = Integer.MAX_VALUE;
		bestLongSideFit = Integer.MAX_VALUE;
		for (int i = 0; i < freeRectangles.size(); ++i) {
			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width >= width && freeRectangles.get(i).height >= height) {
				int leftoverHoriz = Math.abs(freeRectangles.get(i).width - width);
				int leftoverVert = Math.abs(freeRectangles.get(i).height - height);
				int shortSideFit = Math.min(leftoverHoriz, leftoverVert);
				int longSideFit = Math.max(leftoverHoriz, leftoverVert);

				if (shortSideFit < bestShortSideFit
						|| (shortSideFit == bestShortSideFit && longSideFit < bestLongSideFit)) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = width;
					bestNode.height = height;
					bestShortSideFit = shortSideFit;
					bestLongSideFit = longSideFit;
				}
			}

			if (binAllowFlip && freeRectangles.get(i).width >= height && freeRectangles.get(i).height >= width) {
				int flippedLeftoverHoriz = Math.abs(freeRectangles.get(i).width - height);
				int flippedLeftoverVert = Math.abs(freeRectangles.get(i).height - width);
				int flippedShortSideFit = Math.min(flippedLeftoverHoriz, flippedLeftoverVert);
				int flippedLongSideFit = Math.max(flippedLeftoverHoriz, flippedLeftoverVert);

				if (flippedShortSideFit < bestShortSideFit
						|| (flippedShortSideFit == bestShortSideFit && flippedLongSideFit < bestLongSideFit)) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = height;
					bestNode.height = width;
					bestShortSideFit = flippedShortSideFit;
					bestLongSideFit = flippedLongSideFit;
				}
			}
		}
		return bestNode;
	}

	private Rect FindPositionForNewNodeBestLongSideFit(int width, int height, int bestShortSideFit,
			int bestLongSideFit) {
		Rect bestNode = new Rect();

		bestShortSideFit = Integer.MAX_VALUE;
		bestLongSideFit = Integer.MAX_VALUE;

		for (int i = 0; i < freeRectangles.size(); ++i) {
			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width >= width && freeRectangles.get(i).height >= height) {
				int leftoverHoriz = Math.abs(freeRectangles.get(i).width - width);
				int leftoverVert = Math.abs(freeRectangles.get(i).height - height);
				int shortSideFit = Math.min(leftoverHoriz, leftoverVert);
				int longSideFit = Math.max(leftoverHoriz, leftoverVert);

				if (longSideFit < bestLongSideFit
						|| (longSideFit == bestLongSideFit && shortSideFit < bestShortSideFit)) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = width;
					bestNode.height = height;
					bestShortSideFit = shortSideFit;
					bestLongSideFit = longSideFit;
				}
			}

			if (binAllowFlip && freeRectangles.get(i).width >= height && freeRectangles.get(i).height >= width) {
				int leftoverHoriz = Math.abs(freeRectangles.get(i).width - height);
				int leftoverVert = Math.abs(freeRectangles.get(i).height - width);
				int shortSideFit = Math.min(leftoverHoriz, leftoverVert);
				int longSideFit = Math.max(leftoverHoriz, leftoverVert);

				if (longSideFit < bestLongSideFit
						|| (longSideFit == bestLongSideFit && shortSideFit < bestShortSideFit)) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = height;
					bestNode.height = width;
					bestShortSideFit = shortSideFit;
					bestLongSideFit = longSideFit;
				}
			}
		}
		return bestNode;
	}

	Rect FindPositionForNewNodeBestAreaFit(int width, int height, int bestAreaFit, int bestShortSideFit) {
		Rect bestNode = new Rect();
		bestAreaFit = Integer.MAX_VALUE;
		bestShortSideFit = Integer.MAX_VALUE;
		for (int i = 0; i < freeRectangles.size(); ++i) {
			int areaFit = freeRectangles.get(i).width * freeRectangles.get(i).height - width * height;

			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width >= width && freeRectangles.get(i).height >= height) {
				int leftoverHoriz = Math.abs(freeRectangles.get(i).width - width);
				int leftoverVert = Math.abs(freeRectangles.get(i).height - height);
				int shortSideFit = Math.min(leftoverHoriz, leftoverVert);

				if (areaFit < bestAreaFit || (areaFit == bestAreaFit && shortSideFit < bestShortSideFit)) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = width;
					bestNode.height = height;
					bestShortSideFit = shortSideFit;
					bestAreaFit = areaFit;
				}
			}

			if (binAllowFlip && freeRectangles.get(i).width >= height && freeRectangles.get(i).height >= width) {
				int leftoverHoriz = Math.abs(freeRectangles.get(i).width - height);
				int leftoverVert = Math.abs(freeRectangles.get(i).height - width);
				int shortSideFit = Math.min(leftoverHoriz, leftoverVert);

				if (areaFit < bestAreaFit || (areaFit == bestAreaFit && shortSideFit < bestShortSideFit)) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = height;
					bestNode.height = width;
					bestShortSideFit = shortSideFit;
					bestAreaFit = areaFit;
				}
			}
		}
		return bestNode;
	}

	Rect FindPositionForNewNodeBestSquareFit(int width, int height, int score1, int score2) {
		score1 = Integer.MIN_VALUE;
		score2 = Integer.MAX_VALUE;
		Rect bestNode = new Rect();
		for (int i = 0; i < freeRectangles.size(); ++i) {
			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width >= width && freeRectangles.get(i).height >= height) {
				int xbound = freeRectangles.get(i).x + width;
				int ybound = freeRectangles.get(i).y + height;
				int shortSideFit = Math.min(xbound, ybound);
				int longSideFit = Math.max(xbound, ybound);
				if (longSideFit < score2 || (longSideFit == score2 && shortSideFit < score1)) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = width;
					bestNode.height = height;
					score1 = shortSideFit;
					score2 = longSideFit;
				}
			}
		}
		return bestNode;
	}

	int CommonIntervalLength(int i1start, int i1end, int i2start, int i2end) {
		if (i1end < i2start || i2end < i1start)
			return 0;
		return Math.min(i1end, i2end) - Math.max(i1start, i2start);
	}

	/// Computes the placement score for the -CP variant.
	int ContactPointScoreNode(int x, int y, int width, int height) {
		int score = 0;

		if (x == 0 || x + width == binWidth)
			score += height;
		if (y == 0 || y + height == binHeight)
			score += width;

		for (int i = 0; i < usedRectangles.size(); ++i) {
			if (usedRectangles.get(i).x == x + width || usedRectangles.get(i).x + usedRectangles.get(i).width == x)
				score += CommonIntervalLength(usedRectangles.get(i).y,
						usedRectangles.get(i).y + usedRectangles.get(i).height, y, y + height);
			if (usedRectangles.get(i).y == y + height || usedRectangles.get(i).y + usedRectangles.get(i).height == y)
				score += CommonIntervalLength(usedRectangles.get(i).x,
						usedRectangles.get(i).x + usedRectangles.get(i).width, x, x + width);
		}
		return score;

	}

	private Rect FindPositionForNewNodeContactPoint(int width, int height, int bestContactScore) {
		Rect bestNode = new Rect();

		bestContactScore = -1;

		for (int i = 0; i < freeRectangles.size(); ++i) {
			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width >= width && freeRectangles.get(i).height >= height) {
				int score = ContactPointScoreNode(freeRectangles.get(i).x, freeRectangles.get(i).y, width, height);
				if (score > bestContactScore) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = width;
					bestNode.height = height;
					bestContactScore = score;
				}
			}
			if (freeRectangles.get(i).width >= height && freeRectangles.get(i).height >= width) {
				int score = ContactPointScoreNode(freeRectangles.get(i).x, freeRectangles.get(i).y, height, width);
				if (score > bestContactScore) {
					bestNode.x = freeRectangles.get(i).x;
					bestNode.y = freeRectangles.get(i).y;
					bestNode.width = height;
					bestNode.height = width;
					bestContactScore = score;
				}
			}
		}
		return bestNode;
	}

	/// @return True if the free node was split.
	boolean SplitFreeNode(Rect freeNode, Rect usedNode) {
		Rect newNode;

		// Test with SAT if the rectangles even intersect.
		if (usedNode.x >= freeNode.x + freeNode.width || usedNode.x + usedNode.width <= freeNode.x
				|| usedNode.y >= freeNode.y + freeNode.height || usedNode.y + usedNode.height <= freeNode.y)
			return false;

		if (usedNode.x < freeNode.x + freeNode.width && usedNode.x + usedNode.width > freeNode.x) {
			// New node at the top side of the used node.
			if (usedNode.y > freeNode.y && usedNode.y < freeNode.y + freeNode.height) {
				newNode = freeNode.clone();
				newNode.height = usedNode.y - newNode.y;
				freeRectangles.add(newNode);
			}

			// New node at the bottom side of the used node.
			if (usedNode.y + usedNode.height < freeNode.y + freeNode.height) {
				newNode = freeNode.clone();
				newNode.y = usedNode.y + usedNode.height;
				newNode.height = freeNode.y + freeNode.height - (usedNode.y + usedNode.height);
				freeRectangles.add(newNode);
			}
		}

		if (usedNode.y < freeNode.y + freeNode.height && usedNode.y + usedNode.height > freeNode.y) {
			// New node at the left side of the used node.
			if (usedNode.x > freeNode.x && usedNode.x < freeNode.x + freeNode.width) {
				newNode = freeNode.clone();
				newNode.width = usedNode.x - newNode.x;
				freeRectangles.add(newNode);
			}

			// New node at the right side of the used node.
			if (usedNode.x + usedNode.width < freeNode.x + freeNode.width) {
				newNode = freeNode.clone();
				newNode.x = usedNode.x + usedNode.width;
				newNode.width = freeNode.x + freeNode.width - (usedNode.x + usedNode.width);
				freeRectangles.add(newNode);
			}
		}

		return true;

	}

	/// Goes through the free rectangle list and removes any redundant entries.
	void PruneFreeList() {
		for (int i = 0; i < freeRectangles.size(); ++i) {
			for (int j = i + 1; j < freeRectangles.size(); ++j) {
				if (IsContainedIn(freeRectangles.get(i), freeRectangles.get(j))) {
					freeRectangles.remove(i);
					--i;
					break;
				}
				if (IsContainedIn(freeRectangles.get(j), freeRectangles.get(i))) {
					freeRectangles.remove(j);
					--j;
				}
			}
		}
	}

	public boolean IsContainedIn(Rect a, Rect b) {
		return a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height;
	}
}

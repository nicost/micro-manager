package org.micromanager.plugins.mist.lib;

/**
 * LinearBlender class for blending tiles.
 * This class calculates the blending weights for each pixel in the tile.
 * The blending weights are calculated based on the distance to the nearest edge of the tile.
 * The blending weights are used to blend the tiles together.
 */
public class LinearBlender {
   private static final double DEFAULT_ALPHA = 1.5;
   private final double[][] lookupTable;

   /**
    * Create a new LinearBlender.
    *
    * @param imgWidth  the width of the image
    * @param imgHeight the height of the image
    * @param alpha     the alpha component of the linear blend
    */
   public LinearBlender(int imgWidth, int imgHeight, double alpha) {

      if (Double.isNaN(alpha)) {
         alpha = DEFAULT_ALPHA;
      }

      this.lookupTable = new double[imgHeight][imgWidth];
      for (int i = 0; i < imgHeight; i++) {
         for (int j = 0; j < imgWidth; j++) {
            this.lookupTable[i][j] = getWeight(i, j, imgWidth, imgHeight, alpha);
         }
      }
   }

   private static double getWeight(int row, int col, int imWidth, int imHeight, double alpha) {
      double distWest = col + 1.0;
      double distNorth = row + 1.0;
      double distEast = imWidth - col;
      double distSouth = imHeight - row;
      double minEastWest = Math.min(distEast, distWest);
      double minNorthSouth = Math.min(distNorth, distSouth);
      double weight = minEastWest * minNorthSouth;

      return Math.pow(weight, alpha);
   }

   public double[][] getLookupTable() {
      return lookupTable;
   }

}

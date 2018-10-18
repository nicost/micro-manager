package org.micromanager.acquiremultipleregions;

import java.io.File;
import org.micromanager.MultiStagePosition;
import org.micromanager.PositionList;
import org.micromanager.internal.utils.TileCreator;

/**
 *
 * @author kthorn
 */
class Region {

   public PositionList positions;
   public String directory;
   public String filename;

   public Region(PositionList PL, String directory, String filename) {
      this.positions = PositionList.newInstance(PL);
      this.directory = directory;
      this.filename = filename;
   }

   /*
    Return a name for the region, by concatenating Directory and Filename
    */
   public String name() {
      File loc = new File(directory, filename);
      String fullfile = loc.getPath();
      return fullfile;
   }

   public MultiStagePosition center() {
      double centerX;
      double centerY;
      MultiStagePosition centerPos;
      PositionList PL = TileCreator.boundingBox(positions);
      MultiStagePosition minCoords = PL.getPosition(0);
      MultiStagePosition maxCoords = PL.getPosition(1);
      centerX = (minCoords.getX() + maxCoords.getX()) / 2;
      centerY = (minCoords.getY() + maxCoords.getY()) / 2;
      centerPos = new MultiStagePosition(minCoords.getDefaultXYStage(),
              centerX, centerY, minCoords.getDefaultZStage(), minCoords.getZ());
      return centerPos;
   }

   public int getNumXTiles(double xStepSize) {
      PositionList bBox;
      double minX, maxX;
      int numXImages;

      bBox = TileCreator.boundingBox(positions);
      minX = bBox.getPosition(0).getX();
      maxX = bBox.getPosition(1).getX();
      numXImages = (int) Math.ceil(Math.abs(maxX - minX) / xStepSize) + 1; // +1 for fencepost problem
      return numXImages;
   }

   public int getNumYTiles(double yStepSize) {
      PositionList bBox;
      double minY, maxY;
      int numYImages;

      bBox = TileCreator.boundingBox(positions);
      minY = bBox.getPosition(0).getY();
      maxY = bBox.getPosition(1).getY();
      numYImages = (int) Math.ceil(Math.abs(maxY - minY) / yStepSize) + 1; // +1 for fencepost problem
      return numYImages;
   }

   }
}

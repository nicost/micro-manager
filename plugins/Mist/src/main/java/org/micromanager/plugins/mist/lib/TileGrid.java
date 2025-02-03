package org.micromanager.plugins.mist.lib;

import ij.process.ImageProcessor;
import java.io.IOException;
import org.micromanager.data.Image;
import org.micromanager.data.internal.DefaultImageJConverter;


/**
 * Overview of the ImageTiles used in the larger image.
 */
public class TileGrid {

   /**
    * The blending mode to use when blending tiles.
    */
   public enum BlendMode {
      NONE, AVERAGE, LINEAR
   }

   private final int nrRows_;
   private final int nrCols_;
   private final ImageTile[][] tiles_;

   /**
    * Create a new TileGrid.
    *
    * @param nrRows Number of Rows in the grid
    * @param nrCols Number of Columns in the grid
    */
   public TileGrid(int nrRows, int nrCols) {
      nrRows_ = nrRows;
      nrCols_ = nrCols;
      tiles_ = new ImageTile[nrRows][nrCols];
   }

   public void setTile(int row, int col, ImageTile tile) {
      tiles_[row][col] = tile;
   }

   public ImageTile getTile(int row, int col) {
      return tiles_[row][col];
   }

   /**
    * Assign the neighbors of each tile in the grid.
    * Should be called only after all tiles have been added.
    */
   public void assignNeighbors() {
      for (int row = 0; row < nrRows_; row++) {
         for (int col = 0; col < nrCols_; col++) {
            ImageTile tile = tiles_[row][col];
            if (row < nrRows_ - 1) {
               tile.setSouthNeighbor(tiles_[row + 1][col]);
            }
            if (col < nrCols_ - 1) {
               tile.setEastNeighbor(tiles_[row][col + 1]);
            }
            if (row < nrRows_ - 1 && col < nrCols_ - 1) {
               tile.setSouthEastNeighbor(tiles_[row + 1][col + 1]);
            }
         }
      }
   }

   /**
    * Place all tiles into the large image in a dumb way.
    * Each consecutive tile will overlay the previous one.
    *
    * @param largeIp the large image processor
    * @throws IOException if the tile can not be opened
    */
   public void placeTiles(ImageProcessor largeIp) throws IOException {
      for (int row = 0; row < nrRows_; row++) {
         for (int col = 0; col < nrCols_; col++) {
            ImageTile tile = tiles_[row][col];
            if (tile == null) {
               // TODO: throw exception
            }
            Image img = tile.getImage();
            ImageProcessor ip = DefaultImageJConverter.createProcessor(img, false);
            largeIp.insert(ip, tile.getTileOriginX(), tile.getTileOriginY());
         }
      }
   }

   /**
    * Blend the tiles in the grid using the specified mode.
    * Tiles should first be placed in the largeIp using placeTiles.
    *
    * @param largeIp ImageProcessor of the large image
    * @param mode BlendMode to use
    * @throws IOException if the tile can not be opened
    */
   public void blendTiles(ImageProcessor largeIp, BlendMode mode) throws IOException {
      if (mode == BlendMode.NONE) {
         return;
      }
      double[][] lookupTable = null;
      if (mode == BlendMode.LINEAR) {
         Image tileImg = tiles_[0][0].getImage();
         // TODO: get user input about alpha
         LinearBlender blender = new LinearBlender(tileImg.getWidth(), tileImg.getHeight(), 1.5);
         lookupTable = blender.getLookupTable();
      } else {
         // blendAverage();
      }
      // it should be possible to run this on multiple threads, for instance, one per row
      for (int row = 0; row < nrRows_ - 1; row++) {
         for (int col = 0; col < nrCols_ - 1; col++) {
            ImageTile tile = tiles_[row][col];
            ImageProcessor tileProc = DefaultImageJConverter.createProcessor(
                  tile.getImage(), false);
            ImageTile east = tile.getEastNeighbor();
            ImageProcessor eastProc = DefaultImageJConverter.createProcessor(
                  east.getImage(), false);
            ImageTile south = tile.getSouthNeighbor();
            ImageProcessor southProc = DefaultImageJConverter.createProcessor(
                  south.getImage(), false);
            ImageTile southEast = tile.getSouthEastNeighbor();
            ImageProcessor southEastProc = DefaultImageJConverter.createProcessor(
                  southEast.getImage(), false);

               // there are 3 parts of overlap, east only, south only, and east and south

               // east only, placed in a block so that we can reuse variable names
               {
               int xStart = east.getTileOriginX() - tile.getTileOriginX();
               int yStart = east.getTileOriginY() - tile.getTileOriginY();
               int largeXStart = tile.getTileOriginX() + xStart;
               int largeYStart = tile.getTileOriginY() + yStart;
               int xLength = tile.getImage().getWidth() - xStart;
               int yLength = south.getTileOriginY() - tile.getTileOriginY() - yStart;
               for (int x = 0; x < xLength; x++) {
                  for (int y = 0; y < yLength; y++) {
                     if (yStart + y < 0) {
                        continue;
                     }
                     int tileValue = tileProc.get(x + xStart, y - yStart);
                     int eastValue = eastProc.get(x, y);
                     if (mode == BlendMode.LINEAR) {
                        double tileWeight = lookupTable[x + xStart][y + yStart];
                        double eastWeight = lookupTable[x][y];
                        double weight = tileWeight + eastWeight;
                        double sums = tileWeight * tileValue + eastWeight * eastValue;
                        int value = (int) (sums / weight);
                        largeIp.set(x + largeXStart, y + largeYStart, value);
                     } else if (mode == BlendMode.AVERAGE) {
                        int value = (tileValue + eastValue) / 2;
                        largeIp.set(x + largeXStart, y + largeYStart, value);
                     }
                  }
               }
               }

               // south only
               {
               int xStart = south.getTileOriginX() - tile.getTileOriginX();
               int yStart = south.getTileOriginY() - tile.getTileOriginY();
               int largeXStart = tile.getTileOriginX() + xStart;
               int largeYStart = tile.getTileOriginY() + yStart;
               int xLength = east.getTileOriginX() - tile.getTileOriginX() - xStart;
               int yLength = tile.getImage().getHeight() - yStart;
               for (int x = 0; x < xLength; x++) {
                  if (x + xStart < 0) {
                     continue;
                  }
                  for (int y = 0; y < yLength; y++) {
                     int tileValue = tileProc.get(x + xStart, y - yStart);
                     int southValue = southProc.get(x, y);
                     if (mode == BlendMode.LINEAR) {
                        double tileWeight = lookupTable[x + xStart][y + yStart];
                        double southWeight = lookupTable[x][y];
                        double weight = tileWeight + southWeight;
                        double sums = tileWeight * tileValue + southWeight * southValue;
                        int value = (int) (sums / weight);
                        largeIp.set(x + largeXStart, y + largeYStart, value);
                     } else if (mode == BlendMode.AVERAGE) {
                        int value = (tileValue + southValue) / 2;
                        largeIp.set(x + largeXStart, y + largeYStart, value);
                     }
                  }
               }
               }

               // south and east corner
               {
               int tileOriginX = east.getTileOriginX();
               if (tileOriginX > southEast.getTileOriginX()) {
                  tileOriginX = southEast.getTileOriginX();
               }
               int xStart = tileOriginX - tile.getTileOriginX();
               int tileOriginY = south.getTileOriginY();
               if (tileOriginY > southEast.getTileOriginY()) {
                  tileOriginY = southEast.getTileOriginY();
               }
               int yStart = tileOriginY - tile.getTileOriginY();
               int largeXStart = tile.getTileOriginX() + xStart;
               int largeYStart = tile.getTileOriginY() + yStart;
               int xLength = tile.getImage().getWidth() - xStart;
               int yLength = tile.getImage().getHeight() - yStart;
               for (int x = 0; x < xLength; x++) {
                  if (x + xStart < 0) {
                     continue;
                  }
                  for (int y = 0; y < yLength; y++) {
                     if (y + yStart < 0) {
                        continue;
                     }
                     int tileValue = tileProc.get(x + xStart, y - yStart);
                     Integer southValue = null;
                     Integer eastValue = null;
                     Integer southEastValue = null;
                     int largeX = x + largeXStart;
                     int largeY = y
                     if (largeXStart + x
                     int southValue = southProc.get(x, y);
                     if (mode == BlendMode.LINEAR) {
                        double tileWeight = lookupTable[x + xStart][y + yStart];
                        double southWeight = lookupTable[x][y];
                        double weight = tileWeight + southWeight;
                        double sums = tileWeight * tileValue + southWeight * southValue;
                        int value = (int) (sums / weight);
                        largeIp.set( largeX, y + largeYStart, value);
                     } else if (mode == BlendMode.AVERAGE) {
                        int value = (tileValue + southValue) / 2;
                        largeIp.set(x + largeXStart, y + largeYStart, value);
                     }
                  }
               }
               }

         }
      }
   }



}
package org.micromanager.plugins.mist.lib;

import java.io.IOException;
import org.micromanager.data.Coords;
import org.micromanager.data.DataProvider;
import org.micromanager.data.Image;


/**
 * Treat an image as a tile of a larger image.
 * This class holds the keys to get access to the image data, and the
 * coordinates of the tile in the large image.
 * Tiles are organized in a grid, starting in the top left corner (northwest corner).
 * The ImageTile class also holds references to its east and south neighbors in the grid.
 */
public class ImageTile {
   private final DataProvider dataProvider_;
   private final Coords coords_;
   private int tileOriginX_; // x-coordinate of the top-left corner of the tile in the large image
   private int tileOriginY_; // y-coordinate of the top-left corner of the tile in the large image
   private ImageTile eastNeighbor_;
   private ImageTile southNeighbor_;
   private ImageTile southEastNeighbor_;


   /**
    * Create a new ImageTile.
    *
    * @param dataProvider the data provider that holds the image data
    * @param coords the coordinates of the tile in the large image
    * @param tileOriginX the x-coordinate of the top-left corner of the tile in the large image
    * @param tileOriginY the y-coordinate of the top-left corner of the tile in the large image
    */
   public ImageTile(DataProvider dataProvider, Coords coords, int tileOriginX, int tileOriginY) {
      dataProvider_ = dataProvider;
      coords_ = coords;
      tileOriginX_ = tileOriginX;
      tileOriginY_ = tileOriginY;
   }

   public Image getImage() throws IOException {
      return dataProvider_.getImage(coords_);
   }

   public void setEastNeighbor(ImageTile eastNeighbor) {
      eastNeighbor_ = eastNeighbor;
   }

   public void setSouthNeighbor(ImageTile southNeighbor) {
      southNeighbor_ = southNeighbor;
   }

   public void setSouthEastNeighbor(ImageTile southEastNeighbor) {
      southEastNeighbor_ = southEastNeighbor;
   }

   public ImageTile getEastNeighbor() {
      return eastNeighbor_;
   }

   public ImageTile getSouthNeighbor() {
      return southNeighbor_;
   }

   public ImageTile getSouthEastNeighbor() {
      return southEastNeighbor_;
   }

   public void resetTileOrigin(int tileOriginX, int tileOriginY) {
      tileOriginX_ = tileOriginX;
      tileOriginY_ = tileOriginY;
   }

   public int getTileOriginX() {
      return tileOriginX_;
   }

   public int getTileOriginY() {
      return tileOriginY_;
   }

}
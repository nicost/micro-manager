// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:47:52 PM EST
//
// Time-stamp: <Aug 1, 2013 3:47:52 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.tilegrid.traverser;


import org.micromanager.imageprocessing.mist.lib.imagetile.ImageTile;
import org.micromanager.imageprocessing.mist.lib.tilegrid.TileGrid;

/**
 * Traversal utility function for creating a traversal based on a type bound to a grid of tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridTraverserFactory {

   /**
    * Generates a traversal surrounding a tile grid
    *
    * @param type the type of traversal
    * @param grid the subgrid to traverse
    * @return the traverser
    */
   public static <T extends ImageTile<?>> TileGridTraverser<T> makeTraverser(
         TileGridTraverser.Traversals type, TileGrid<T> grid) {
      switch (type) {
         case ROW:
            return new TileGridRowTraverser<T>(grid);
         case COLUMN:
            return new TileGridColumnTraverser<T>(grid);
         case COLUMN_CHAINED:
            return new TileGridColumnChainedTraverser<T>(grid);
         case DIAGONAL:
            return new TileGridDiagonalTraverser<T>(grid);
         case DIAGONAL_CHAINED:
            return new TileGridDiagonalChainedTraverser<T>(grid);
         case ROW_CHAINED:
            return new TileGridRowChainedTraverser<T>(grid);
         default:
            return new TileGridRowTraverser<T>(grid);

      }
   }

}

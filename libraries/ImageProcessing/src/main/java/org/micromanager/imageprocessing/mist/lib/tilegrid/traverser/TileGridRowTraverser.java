
// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:48:00 PM EST
//
// Time-stamp: <Aug 1, 2013 3:48:00 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.tilegrid.traverser;


import java.util.Iterator;
import org.micromanager.imageprocessing.mist.lib.imagetile.ImageTile;
import org.micromanager.imageprocessing.mist.lib.tilegrid.TileGrid;


/**
 * Traversal type for traversing by row (combed).
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridRowTraverser<T extends ImageTile<?>>
      implements TileGridTraverser<T>, Iterator<T> {

   private TileGrid<T> subGrid;
   private int currentRowPosition;
   private int currentColumnPosition;
   private int linear;

   /**
    * Initializes a row traverser given a subgrid.
    * */
   public TileGridRowTraverser(TileGrid<T> subgrid) {
      this.subGrid = subgrid;
      this.currentRowPosition = 0;
      this.currentColumnPosition = 0;
      this.linear = 0;
   }

   @Override
   public Iterator<T> iterator() {
      return this;
   }

   /**
    * Gets the current row of the traverser.
    */
   @Override
   public int getCurrentRow() {
      return this.currentRowPosition;
   }

   /**
    * Gets the current column of the traverser.
    */
   @Override
   public int getCurrentColumn() {
      return this.currentColumnPosition;
   }

   @Override
   public String toString() {
      return "Traversing by row: " + this.subGrid;
   }

   @Override
   public boolean hasNext() {
      return this.linear < this.subGrid.getSubGridSize();
   }

   @Override
   public T next() {
      final T actualTile =
            this.subGrid.getTile(this.currentRowPosition + this.subGrid.getStartRow(),
                  this.currentColumnPosition
                  + this.subGrid.getStartCol());

      this.linear++;

      this.currentColumnPosition++;

      if (this.currentColumnPosition >= this.subGrid.getExtentWidth()) {
         this.currentColumnPosition = 0;
         this.currentRowPosition++;
      }

      return actualTile;
   }

   @Override
   public void remove() {
      // Not implemented/not needed
   }

}

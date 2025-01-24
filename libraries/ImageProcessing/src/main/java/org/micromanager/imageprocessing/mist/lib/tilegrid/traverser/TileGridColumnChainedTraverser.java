
// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:39:44 PM EST
//
// Time-stamp: <Aug 1, 2013 3:39:44 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.tilegrid.traverser;

import java.util.Iterator;
import org.micromanager.imageprocessing.mist.lib.imagetile.ImageTile;
import org.micromanager.imageprocessing.mist.lib.tilegrid.TileGrid;



/**
 * Traversal type for traversing a grid column chained.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridColumnChainedTraverser<T extends ImageTile<?>>
      implements TileGridTraverser<T>, Iterator<T> {

   private TileGrid<T> subGrid;
   private int currentRowPosition;
   private int currentColumnPosition;

   private int linear;
   private int dir;

   /**
    * Initializes a column-chained traverser given a subgrid.
    */
   public TileGridColumnChainedTraverser(TileGrid<T> subgrid) {
      this.subGrid = subgrid;
      this.currentRowPosition = 0; // subGrid.getStartRow();
      this.currentColumnPosition = 0; // subGrid.getStartCol();
      this.linear = 0;
      this.dir = 0;
   }

   @Override
   public Iterator<T> iterator() {
      return this;
   }

   /**
    * Gets the current row for the traverser.
    */
   @Override
   public int getCurrentRow() {
      return this.currentRowPosition;
   }

   /**
    * Gets the current column for the traverser.
    */
   @Override
   public int getCurrentColumn() {
      return this.currentColumnPosition;
   }

   @Override
   public String toString() {
      return "Traversing by column chained: " + this.subGrid;
   }

   @Override
   public boolean hasNext() {
      return this.linear < this.subGrid.getSubGridSize();
   }

   @Override
   public T next() {
      int tempRowPos = this.currentRowPosition;

      final T actualTile =
            this.subGrid.getTile(this.currentRowPosition + this.subGrid.getStartRow(),
                  this.currentColumnPosition
                  + this.subGrid.getStartCol());

      this.linear++;

      this.currentRowPosition += this.dir;

      if (this.currentRowPosition < 0 || this.currentRowPosition
            >= this.subGrid.getExtentHeight()) {
         this.dir = -this.dir;
         this.currentColumnPosition++;
         this.currentRowPosition = tempRowPos;
      }

      return actualTile;
   }

   @Override
   public void remove() {
      // Not implemented/not needed

   }
}

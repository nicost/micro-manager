

// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:39:33 PM EST
//
// Time-stamp: <Aug 1, 2013 3:39:33 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.tilegrid.traverser;


import java.util.Iterator;
import org.micromanager.imageprocessing.mist.lib.imagetile.ImageTile;
import org.micromanager.imageprocessing.mist.lib.tilegrid.TileGrid;



/**
 * Traversal type for traversing a grid diagonal chained
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridDiagonalChainedTraverser<T extends ImageTile<?>>
      implements TileGridTraverser<T>, Iterator<T> {

   private TileGrid<T> subGrid;
   private int currentRowPosition;
   private int currentColumnPosition;
   private int linear;

   private int dirRow;
   private int dirCol;

   /**
    * Initializes a diagonal-chained traverser given a subgrid
    */
   public TileGridDiagonalChainedTraverser(TileGrid<T> subgrid) {
      this.subGrid = subgrid;
      this.currentRowPosition = 0;
      this.currentColumnPosition = 0;
      this.linear = 0;
      this.dirRow = -1;
      this.dirCol = 1;
   }

   @Override
   public Iterator<T> iterator() {
      return this;
   }

   /**
    * Gets the current row of the traverser
    */
   @Override
   public int getCurrentRow() {
      return this.currentRowPosition;
   }

   /**
    * Gets the current column of the traverser
    */
   @Override
   public int getCurrentColumn() {
      return this.currentColumnPosition;
   }

   @Override
   public String toString() {
      return "Traversing by diagonal chained: " + this.subGrid;
   }

   @Override
   public boolean hasNext() {
      return this.linear < this.subGrid.getSubGridSize();
   }

   @Override
   public T next() {
      final int tempRow = this.currentRowPosition;
      final int tempCol = this.currentColumnPosition;

      final T actualTile =
            this.subGrid.getTile(this.currentRowPosition + this.subGrid.getStartRow(), this.currentColumnPosition
                  + this.subGrid.getStartCol());

      this.linear++;

      this.currentRowPosition += this.dirRow;
      this.currentColumnPosition += this.dirCol;

      if (hasNext()) {
         if (this.currentColumnPosition < 0 || this.currentColumnPosition >= this.subGrid.getExtentWidth()) {
            this.currentRowPosition = tempRow + 1;
            this.currentColumnPosition = tempCol;

            if (this.currentRowPosition >= this.subGrid.getExtentHeight()) {
               this.currentRowPosition = tempRow;
               this.currentColumnPosition = tempCol + 1;
            }

            this.dirRow = -this.dirRow;
            this.dirCol = -this.dirCol;
         } else if (this.currentRowPosition < 0 || this.currentRowPosition >= this.subGrid.getExtentHeight()) {
            this.currentRowPosition = tempRow;
            this.currentColumnPosition = tempCol + 1;
            this.dirRow = -this.dirRow;
            this.dirCol = -this.dirCol;
         }
      }

      return actualTile;
   }

   @Override
   public void remove() {
      // Not implemented/not needed
   }

}


// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:39:26 PM EST
//
// Time-stamp: <Aug 1, 2013 3:39:26 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.tilegrid.traverser;


import java.util.Iterator;
import org.micromanager.imageprocessing.mist.lib.imagetile.ImageTile;
import org.micromanager.imageprocessing.mist.lib.tilegrid.TileGrid;


/**
 * Traversal type for traversing by diagonal.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileGridDiagonalTraverser<T extends ImageTile<?>>
      implements TileGridTraverser<T>, Iterator<T> {

   private TileGrid<T> subGrid;
   private int currentRowPosition;
   private int currentColumnPosition;

   private int linear;

   private int diagRow;
   private int diagCol;
   private int diagDirRow;
   private int diagDirCol;

   /**
    * Initializes a diagonal traverser given a subgrid.
    */
   public TileGridDiagonalTraverser(TileGrid<T> subgrid) {
      this.subGrid = subgrid;
      this.currentRowPosition = 0;
      this.currentColumnPosition = 0;
      this.linear = 0;

      this.diagRow = 0;
      this.diagCol = 0;
      this.diagDirRow = 0;
      this.diagDirCol = 1;
   }

   @Override
   public Iterator<T> iterator() {
      return this;
   }

   @Override
   public int getCurrentRow() {
      return this.currentRowPosition;
   }

   @Override
   public int getCurrentColumn() {
      return this.currentColumnPosition;
   }

   @Override
   public String toString() {
      return "Traversing by diagonal: " + this.subGrid;
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

      this.currentRowPosition++;
      this.currentColumnPosition--;

      if (hasNext()) {
         if (this.currentColumnPosition < 0 || this.currentRowPosition
               >= this.subGrid.getExtentHeight()) {
            int diagNextRow = this.diagRow + this.diagDirRow;
            int diagNextCol = this.diagCol + this.diagDirCol;

            if (diagNextCol == this.subGrid.getExtentWidth()) {
               this.diagDirRow = 1;
               this.diagDirCol = 0;

               diagNextRow = this.diagRow + this.diagDirRow;
               diagNextCol = this.diagCol + this.diagDirCol;
            }

            this.diagRow = diagNextRow;
            this.diagCol = diagNextCol;

            this.currentRowPosition = this.diagRow;
            this.currentColumnPosition = this.diagCol;

         }
      }

      return actualTile;
   }

   @Override
   public void remove() {
      // Not implemented/not needed
   }
}

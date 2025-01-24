
// ================================================================
//
// Author: tjb3
// Date: Aug 1, 2013 3:41:17 PM EST
//
// Time-stamp: <Aug 1, 2013 3:41:17 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.tilegrid.traverser;

/**
 * Traversal interface, describing a traverser
 *
 * @author Tim Blattner
 * @version 1.0
 */
public interface TileGridTraverser<T> extends Iterable<T> {

   /**
    * Different types of traversers
    */
   public static enum Traversals {
      /**
       * Row traversal (combed)
       */
      ROW,

      /**
       * Row chained traversal
       */
      ROW_CHAINED,

      /**
       * Column traversal (combed)
       */
      COLUMN,

      /**
       * Column chained traversal
       */
      COLUMN_CHAINED,

      /**
       * Diagonal traversal
       */
      DIAGONAL,

      /**
       * Diagonal chained traversal
       */
      DIAGONAL_CHAINED
   }

   /**
    * Gets the current row of the traverser
    *
    * @return the current row of the traverser
    */
   public int getCurrentRow();

   /**
    * Gets the current column of the traverser
    *
    * @return the current column of hte traverser
    */
   public int getCurrentColumn();

}

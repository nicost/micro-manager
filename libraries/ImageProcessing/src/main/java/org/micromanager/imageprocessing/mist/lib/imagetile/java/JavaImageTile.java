

// ================================================================
//
// Author: tjb3
// Date: May 10, 2013 2:58:58 PM EST
//
// Time-stamp: <May 10, 2013 2:58:58 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.imagetile.java;

import ij.process.ImageProcessor;
import java.io.File;
import org.micromanager.imageprocessing.mist.lib.imagetile.ImageTile;


/**
 * Represents an image tile that uses only java (no native bindings). Must initialize an FFT plan
 * before using.
 *
 * <pre>
 * <code>
 * JavaImageTile.initJavaPlan(initialTile);
 * </pre>
 *
 * </code>
 *
 * @author Tim Blattner
 * @version 2013.08.7
 */
public class JavaImageTile extends ImageTile<float[][]> {

   /*
    * The Java FFT plan
    */
   // public static Fft fftPlan = null;


   /**
    * Creates an image tile in a grid.
    *
    * @param file       the image tile file
    * @param row        the row location in the grid
    * @param col        the column location in the grid
    * @param gridWidth  the width of the tile grid (subgrid)
    * @param gridHeight the height of the tile grid (subgrid)
    * @param startRow   the start row of the tile grid (subgrid)
    * @param startCol   the start column of the tile grid (subgrid)
    */
   public JavaImageTile(File file, int row, int col, int gridWidth, int gridHeight, int startRow,
                        int startCol) {
      super(file, row, col, gridWidth, gridHeight, startRow, startCol);
   }

   /**
    * Creates an image tile from a file
    *
    * @param file the image tile file
    */
   public JavaImageTile(File file) {
      super(file, 0, 0, 1, 1, 0, 0);
   }

   /**
    * Uses an already loaded ImageProcessor to construct the Array2DView.
    *
    * @param ip   ImageProcessor with primary pixel data
    * @param absX X position in the final image
    * @param absY Y position in the final image
    * @param corr the tile correlation (now sure what that is)
    */
   public JavaImageTile(ImageProcessor ip, int absX, int absY, double corr) {
      super(ip, absX, absY, corr);
   }

   @Override
   public void releaseFftMemory() {
      //  if (this.fft != null)
      //    this.fft = null;
   }


   /**
    * Computes this image's FFT
    */
   @Override
   public void computeFft() {
      // if the file does not exists on disk, skip computing the fft
      if (!this.fileExists())
         return;

      // if (fftPlan == null)
      //   initJavaPlan(this);

      // readTile();

      // this.fft =
      //     new float[fftPlan.getFrequencySampling2().getCount()][fftPlan.getFrequencySampling1()
      //         .getCount() * 2];

      // copyAndPadFFT();

      // fftPlan.applyForwardPadded(this.fft);
   }

   /*
    // if the file does not exists on disk, skip computing the fft
    if (!this.fileExists())
      return;

    if (fftPlan == null)
      initJavaPlan(this);

    readTile();

    this.fft =
        new float[fftPlan.getFrequencySampling2().getCount()][fftPlan.getFrequencySampling1()
            .getCount() * 2];

    copyAndPadFFT();

    fftPlan.applyForwardPadded(this.fft);

     */

/*
 * Computes this image's FFT
 *
 * @param pool   pool of memory that we might allocate memory from if it is not loaded
 * @param memory extra memory for input if needed
 */
//@Override
//public void computeFft(DynamicMemoryPool<float[][]> pool, TileWorkerMemory memory) {
    /*

    // if the file does not exists on disk, skip computing the fft
    if (!this.fileExists())
      return;

    readTile();

    if (!super.isMemoryLoaded()) {
      this.fft = pool.getMemory();
      super.setMemoryLoaded(true);
    }


    copyAndPadFFT();

    fftPlan.applyForwardPadded(this.fft);

     */
//}

//@Override
//public void computeFft(DynamicMemoryPool<float[][]> pool, TileWorkerMemory memory, CUstream stream) {
    /*
    computeFft(pool, memory);
     */
//}

/*
 * Copies pixel data into fft array for in place transform.
 *
 private void copyAndPadFFT() {
 int n1 = fftPlan.getFrequencySampling1().getCount() * 2;
 int n2 = fftPlan.getFrequencySampling2().getCount();

 for (int r = 0; r < n2; r++) {
 for (int c = 0; c < n1; c++) {

 if (r < this.getHeight() && c < this.getWidth()) {
 this.fft[r][c] = super.getPixels().getPixelValue(c, r);
 } else {
 this.fft[r][c] = 0.0f;
 }
 }
 }
 }
 */

/*
 * Initializes FFT java plan. This plan might apply padding based on paddedHeight/2
 *
 * @param tile the initial tile to get the width and height
 *
public static void initJavaPlan(ImageTile<?> tile) {
tile.readTile();

Log.msg(LogType.VERBOSE, "Initializing Java FFT Plans.");

fftPlan = new Fft(tile.getWidth(), tile.getHeight());
fftPlan.setComplex(false);
fftPlan.setOverwrite(true);
}

}
*/

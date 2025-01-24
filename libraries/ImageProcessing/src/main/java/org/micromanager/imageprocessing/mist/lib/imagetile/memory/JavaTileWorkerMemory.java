

// ================================================================
//
// Author: tjb3
// Date: May 16, 2014 3:52:20 PM EST
//
// Time-stamp: <May 16, 2014 3:52:20 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.imagetile.memory;

import java.nio.ByteBuffer;
import org.bridj.Pointer;
import org.micromanager.imageprocessing.mist.lib.imagetile.ImageTile;
import org.micromanager.imageprocessing.mist.lib.imagetile.java.JavaImageTile;

/**
 * Represents memory that a Java Tile will be working with
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class JavaTileWorkerMemory extends TileWorkerMemory {

   private float[][] arrayMemory;

   /**
    * Initializes the Java tile worker memory
    *
    * @param initTile the initial tile
    */
   public JavaTileWorkerMemory(ImageTile<?> initTile) {
      super(initTile.getWidth(), initTile.getHeight());

      //int fftWidth = JavaImageTile.fftPlan.getFrequencySampling1().getCount() * 2;
      //int fftHeight = JavaImageTile.fftPlan.getFrequencySampling2().getCount();
      //this.arrayMemory = new float[fftHeight][fftWidth];
   }

   @Override
   public float[][] getArrayMemory() {
      return this.arrayMemory;
   }

   @Override
   public void releaseMemory() {
      this.arrayMemory = null;
   }

   @Override
   public ByteBuffer getImageBuffer() {
      throw new IllegalStateException("getImageBuffer is only used for JCUDA Tile workers");
   }

   @Override
   public ByteBuffer getIndexBuffer() {
      throw new IllegalStateException("getIndexBuffer is only used for JCUDA Tile workers");
   }

   @Override
   public ByteBuffer getFilterBuffer() {
      throw new IllegalStateException("getFilterBuffer is only used for JCUDA Tile workers");
   }

   /*
   @Override
   public CUdeviceptr getFftIn() {
      throw new IllegalStateException("getFftIn is only used for JCUDA Tile workers");
   }

  @Override
  public CUdeviceptr getPcmIn() {
    throw new IllegalStateException("getPcmIn is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getPcm() {
    throw new IllegalStateException("getPcm is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getMaxOut() {
    throw new IllegalStateException("getMaxOut is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getMultiMaxOut() {
    throw new IllegalStateException("getMultiMaxOut is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getMultiIdxOut() {
    throw new IllegalStateException("getMultiIdxOut is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getIdxOut() {
    throw new IllegalStateException("getIdxOut is only used for JCUDA Tile workers");
  }

  @Override
  public CUdeviceptr getIdxFilter() {
    throw new IllegalStateException("getIdxFilter is only used for JCUDA Tile workers");
  }

    */

   @Override
   public Integer[] getIndices() {
      throw new IllegalStateException("getIndices is only used for JCUDA Tile workers");
   }

   @Override
   public Pointer<Double> getPCMPMemory() {
      throw new IllegalStateException("getPCMPMemory is only used for FFTW Tile workers");
   }

   @Override
   public Pointer<Double> getPCMInMemory() {
      throw new IllegalStateException("getPCMInMemory is only used for FFTW Tile workers");
   }

   @Override
   public Pointer<Double> getFFTInP() {
      throw new IllegalStateException("getFFTInP is only used for FFTW Tile workers");
   }


   @Override
   public Pointer<Integer> getPeaks() {
      throw new IllegalStateException("getPeaks is only used for FFTW Tile workers");
   }


}

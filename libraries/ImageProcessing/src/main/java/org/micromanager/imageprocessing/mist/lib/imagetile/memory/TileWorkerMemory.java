
// ================================================================
//
// Author: tjb3
// Date: Apr 11, 2014 11:40:10 AM EST
//
// Time-stamp: <Apr 11, 2014 11:40:10 AM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.imagetile.memory;

import java.nio.ByteBuffer;
import org.bridj.Pointer;
// import jcuda.driver.CUdeviceptr;

/**
 * Class that represents the memory required for stitching a pair of tiles. This class is used as a
 * super class for other TileWorkerMemories.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public abstract class TileWorkerMemory<T> {

   private int width;
   private int height;

   /**
    * Initializes tile worker memory.
    *
    * @param tileWidth  the width of the image tile
    * @param tileHeight the height of the image tile
    */
   public TileWorkerMemory(int tileWidth, int tileHeight) {
      this.width = tileWidth;
      this.height = tileHeight;
   }


   /**
    * @return the width
    */
   public int getWidth() {
      return this.width;
   }

   /**
    * @return the height
    */
   public int getHeight() {
      return this.height;
   }


   /**
    * Releases tile memory.
    */
   public abstract void releaseMemory();

   /**
    * @return the imageBuffer
    */
   public abstract ByteBuffer getImageBuffer();

   /**
    * @return the indexBuffer
    */
   public abstract ByteBuffer getIndexBuffer();

   /**
    * @return the filterBuffer
    */
   public abstract ByteBuffer getFilterBuffer();


   /*
    * @return the fftIn
    */
   // public abstract CUdeviceptr getFftIn();

   /*
    * @return the pcmIn
    */
   // public abstract CUdeviceptr getPcmIn();

   /*
    * @return the pcm
    */
   // public abstract CUdeviceptr getPcm();

   /*
    * @return the maxOut
    */
   // public abstract CUdeviceptr getMaxOut();

   /*
    * @return the multiMaxOut
    */
   // public abstract CUdeviceptr getMultiMaxOut();

   /*
    * @return the multimax index output
    */
   // public abstract CUdeviceptr getMultiIdxOut();

   /*
    * @return the idxOut
    */
   // public abstract CUdeviceptr getIdxOut();

   /*
    * @return the idxFilter
    */
   // public abstract CUdeviceptr getIdxFilter();

   /*
    * Returns a reference to the phase correlation matrix memory
    *
    * @return the pcm memory
    */
   public abstract Pointer<T> getPCMPMemory();

   /*
    * Gets the input phase correlation matrix memory
    *
    * @return the pcm input memory
    */
   public abstract Pointer<T> getPCMInMemory();

   /*
    * @return the FFT In pointer
    */
   public abstract Pointer<T> getFFTInP();

   /*
    * @return the array of indices memory
    */
   public abstract Integer[] getIndices();

   /**
    * @return the peaks memory
    */
   public abstract Pointer<Integer> getPeaks();

   /**
    * @return the array memory
    */
   public abstract float[][] getArrayMemory();

}

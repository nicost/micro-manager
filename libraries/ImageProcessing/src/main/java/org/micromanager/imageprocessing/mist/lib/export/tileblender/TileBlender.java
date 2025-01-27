// This code is derived from the source code of the ImageJ "MIST" plugin, developed
// at the National Institute of Standards and Technology by Tim Blattner.
// the code was modified to make it work in the context of the Micro-Manager application.
// Modifications are licensed under the BSD-3-Clause license, a copy of which is
// included in the repository.
// Modifications are copyright 2025, Regents of the University of California
// Author of the modifications: Nico Stuurman
// The original notice is provided below.

// NIST-developed software is provided by NIST as a public service. You may use, copy and
// distribute copies of the software in any medium, provided that you keep intact this
// entire notice. You may improve, modify and create derivative works of the software or
// any portion of the software, and you may copy and distribute such modifications or works.
// Modified works should carry a notice stating that you changed the software
// and should note the date and nature of any such change. Please explicitly acknowledge the
// National Institute of Standards and Technology as the source of the software.

// NIST-developed software is expressly provided "AS IS." NIST MAKES NO WARRANTY OF ANY
// KIND, EXPRESS, IMPLIED, IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT
// LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
// NON-INFRINGEMENT AND DATA ACCURACY. NIST NEITHER REPRESENTS NOR WARRANTS THAT THE
// OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT ANY DEFECTS
// WILL BE CORRECTED. NIST DOES NOT WARRANT OR MAKE ANY REPRESENTATIONS REGARDING THE
// USE OF  THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED TO THE
// CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.

// You are solely responsible for determining the appropriateness of using and distributing
// the software and you assume all risks associated with its use, including but not limited
// to the risks and costs of program errors, compliance with applicable laws, damage to or
// loss of data, programs or equipment, and the unavailability or interruption of operation.
// This software is not intended to be used in any situation where a failure could cause risk
// of injury or damage to property. The software developed by NIST employees is not subject
// to copyright protection within the United States.


// ================================================================
//
// Author: tjb3
// Date: Apr 25, 2014 4:23:52 PM EST
//
// Time-stamp: <Apr 25, 2014 4:23:52 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.export.tileblender;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import loci.formats.FormatException;
import loci.formats.out.OMETiffWriter;
import org.micromanager.imageprocessing.mist.lib.common.Array2DView;
import org.micromanager.imageprocessing.mist.lib.imagetile.ImageTile;


/**
 * Blending interface for tiles.
 *
 * @author Tim Blattner
 * @version 1.0
 */
public abstract class TileBlender {

   private ImageProcessor ip;
   private ByteBuffer buffer;

   private int numChannels;
   private final int bytesPerPixel;
   private final int imageType;

   private int tileWidth;
   private int tileHeight;

   public TileBlender(int bytesPerPixel, int imageType) {
      this.bytesPerPixel = bytesPerPixel;
      this.imageType = imageType;
      this.tileWidth = 0;
      this.tileHeight = 0;
   }

   private void configureDefaultProcessor(int tileSizeX, int tileSizeY) {
      this.numChannels = 1;
      switch (bytesPerPixel) {
         case 1:
            this.ip = new ByteProcessor(tileSizeX, tileSizeY);
            break;
         case 2:
            this.ip = new ShortProcessor(tileSizeX, tileSizeY);
            break;
         case 4:
         default:
            this.ip = new FloatProcessor(tileSizeX, tileSizeY);
            break;
      }
   }

   private void convertDefaultProcessor(Object pixels) {
      switch (bytesPerPixel) {
         case 1:
            this.buffer.put((byte[]) pixels);
            break;
         case 2:
            ShortBuffer shortBuffer = this.buffer.asShortBuffer();
            shortBuffer.put((short[]) pixels);
            break;
         case 4:
         default:
            FloatBuffer floatBuffer = this.buffer.asFloatBuffer();
            floatBuffer.put((float[]) pixels);
            break;
      }
   }

   public void init(int tileSizeX, int tileSizeY) {
      this.tileWidth = tileSizeX;
      this.tileHeight = tileSizeY;

      this.buffer = ByteBuffer.allocate(tileSizeY * tileSizeX * this.bytesPerPixel);
      this.buffer.order(ByteOrder.BIG_ENDIAN);

      switch (imageType) {
         case ImagePlus.GRAY8:
            this.ip = new ByteProcessor(tileSizeX, tileSizeY);
            this.numChannels = 1;
            break;
         case ImagePlus.GRAY16:
            this.ip = new ShortProcessor(tileSizeX, tileSizeY);
            this.numChannels = 1;
            break;
         case ImagePlus.GRAY32:
            this.ip = new FloatProcessor(tileSizeX, tileSizeY);
            this.numChannels = 1;
            break;
         case ImagePlus.COLOR_RGB:
            this.ip = new ColorProcessor(tileSizeX, tileSizeY);
            this.numChannels = 3;
            break;
         default:
            this.configureDefaultProcessor(tileSizeX, tileSizeY);
            break;
      }

      this.initBlender(tileSizeX, tileSizeY);
   }

   /**
    * Initializes the blending function.
    *
    * @param tileSizeX the tile size along X dimension (width)
    * @param tileSizeY the tile size along Y dimension (height)
    */
   public abstract void initBlender(int tileSizeX, int tileSizeY);

   /**
    * Blends a pixel array into the final image.
    *
    * @param x      the current x position in the final image
    * @param y      the current y position in the final image
    * @param pixels the 2D view of pixels that are being added
    * @param tile   the image tile to blend
    */
   public abstract void blend(int x, int y, Array2DView pixels, ImageTile<?> tile);

   /**
    * Finalizes blending, called just before postProcess.
    * The result of this function should fully populate the ImageProcessor with all necessary values.
    */
   public abstract void finalizeBlend();

   public void setPixelValueChannel(int x, int y, int c, int value) {
      if (this.imageType == ImagePlus.GRAY8 || this.imageType == ImagePlus.COLOR_RGB) {
         this.buffer.put((y * this.tileWidth + x) * this.numChannels + c, (byte) value);
      } else {
         this.ip.set(x, y, value);
      }
   }

   public void setPixelValue(int x, int y, int value) {
      if (this.imageType == ImagePlus.GRAY8) {
         this.buffer.put(y * this.tileWidth + x, (byte) (value & 0xFF));
      } else if (this.imageType == ImagePlus.COLOR_RGB) {
         byte r = (byte) ((value & 16711680) >> 16);
         byte g = (byte) ((value & '\uff00') >> 8);
         byte b = (byte) (value & 255);
         this.buffer.put((y * this.tileWidth + x) * this.numChannels, r);
         this.buffer.put((y * this.tileWidth + x) * this.numChannels + 1, g);
         this.buffer.put((y * this.tileWidth + x) * this.numChannels + 2, b);
      } else {
         this.ip.set(x, y, value);
      }
   }

   /**
    * Applies post-processing functions.
    */
   public void postProcess(int tileX, int tileY, int tileXSize, int tileYSize, OMETiffWriter omeTiffWriter) throws IOException, FormatException {
      this.finalizeBlend();

      // Save to image
      Object pixels = this.ip == null ? null : this.ip.getPixels();

      switch (imageType) {
         case ImagePlus.GRAY8:
         case ImagePlus.COLOR_RGB:
            break;
         case ImagePlus.GRAY16:
            ShortBuffer shortBuffer = this.buffer.asShortBuffer();
            shortBuffer.put((short[]) pixels);
            break;
         case ImagePlus.GRAY32:
            FloatBuffer floatBuffer = this.buffer.asFloatBuffer();
            floatBuffer.put((float[]) pixels);
            break;
         default:
            this.convertDefaultProcessor(pixels);
            break;
      }

      omeTiffWriter.saveBytes(0, this.buffer.array(), tileX, tileY, tileXSize, tileYSize);
   }

   public int getTileWidth() {
      return this.tileWidth;
   }

   public int getTileHeight() {
      return this.tileHeight;
   }

   public ByteBuffer getBuffer() {
      return buffer;
   }

   public int getNumChannels() {
      return numChannels;
   }

   public int getBytesPerPixel() {
      return bytesPerPixel;
   }

   public int getImageType() {
      return imageType;
   }
}

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
// Date: Apr 25, 2014 4:23:18 PM EST
//
// Time-stamp: <Apr 25, 2014 4:23:18 PM tjb3>
//
//
// ================================================================

package org.micromanager.imageprocessing.mist.lib.export.tileblender;

import ij.process.ImageProcessor;
import org.micromanager.imageprocessing.mist.lib.common.Array2DView;
import org.micromanager.imageprocessing.mist.lib.imagetile.ImageTile;

/**
 * Creates an average blending function
 *
 * @author Tim Blattner
 * @version 1.0
 */
public class TileAverageBlend extends TileBlender {

  private double[][][] sums;
  private int[][][] counts;

  public TileAverageBlend(int bytesPerPixel, int imageType) {
    super(bytesPerPixel, imageType);
  }

  @Override
  public void initBlender(int tileSizeX, int tileSizeY)
  {
    this.sums = new double[tileSizeY][tileSizeX][this.getNumChannels()];
    this.counts = new int[tileSizeY][tileSizeX][this.getNumChannels()];
  }

  @Override
  public void blend(int x, int y, Array2DView pixels, ImageTile<?> tile) {
    ImageProcessor ip = tile.getImageProcessor();

    int tileY = 0;
    for (int row = pixels.getStartRow(); row < pixels.getStartRow() + pixels.getViewHeight(); row++) {
      int tileX = 0;
      for (int col = pixels.getStartCol(); col < pixels.getStartCol() + pixels.getViewWidth(); col++) {

        int value = ip.getPixel(col, row);

        if (this.getNumChannels() == 1) {
          this.sums[y + tileY][x + tileX][0] += value;
          this.counts[y + tileY][x + tileX][0] += 1;
        } else {
          int r = (value & 16711680) >> 16;
          int g = (value & '\uff00') >> 8;
          int b = value & 255;
          this.sums[y + tileY][x + tileX][0] += r;
          this.counts[y + tileY][x + tileX][0] += 1;
          this.sums[y + tileY][x + tileX][1] += g;
          this.counts[y + tileY][x + tileX][1] += 1;
          this.sums[y + tileY][x + tileX][2] += b;
          this.counts[y + tileY][x + tileX][2] += 1;
        }

        tileX++;
      }
      tileY++;
    }
  }


  @Override
  public void finalizeBlend() {

    for (int row = 0; row < this.getTileHeight(); row++) {
      for (int col = 0; col < this.getTileWidth(); col++) {
        for (int channel = 0; channel < this.getNumChannels(); channel++) {
          double avgVal = 0.0;
          if (this.counts[row][col][channel] != 0) {
            avgVal = this.sums[row][col][channel] / this.counts[row][col][channel];
          }
          this.setPixelValueChannel(col, row, channel, (int)avgVal);
        }
      }
    }
  }

}

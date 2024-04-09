///////////////////////////////////////////////////////////////////////////////
//PROJECT:       PWS Plugin
//
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nick Anthony, 2021
//
// COPYRIGHT:    Northwestern University, 2021
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//

package org.micromanager.sharpnessinspector;

import ij.process.ImageProcessor;
import java.awt.Rectangle;
import org.micromanager.data.Image;
import org.micromanager.imageprocessing.ImgSharpnessAnalysis;
import org.micromanager.internal.MMStudio;

/**
 *
 * @author Nick Anthony
 */
public class SharpnessEvaluator {
   private final ImgSharpnessAnalysis anl = new ImgSharpnessAnalysis();
   // In my experience Redondo works much better than other methods.
   private ImgSharpnessAnalysis.Method method_ = ImgSharpnessAnalysis.Method.Redondo;

   public void setMethod(ImgSharpnessAnalysis.Method method) {
      method_ = method;
      anl.setComputationMethod(ImgSharpnessAnalysis.Method.valueOf(method.name()));
   }
    
   public ImgSharpnessAnalysis.Method getMethod() {
      return method_;
   }
    
   public double evaluate(Image img, Rectangle r) {
      ImageProcessor proc = MMStudio.getInstance().data().getImageJConverter().createProcessor(img);
      proc.setRoi(r);
      proc = proc.crop();
      return anl.compute(proc);
   }
    
}
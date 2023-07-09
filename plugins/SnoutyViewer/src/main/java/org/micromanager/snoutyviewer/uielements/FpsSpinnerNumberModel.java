///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//
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

package org.micromanager.snoutyviewer.uielements;

import javax.swing.SpinnerNumberModel;

/**
 * Helper class for Spinner.
 *
 * @author nico
 */
public class FpsSpinnerNumberModel extends SpinnerNumberModel  {

   private static final int BIGSTEP = 5;
   private static final int SMALLSTEP = 1;
   private static final int CUTOFF = 10;
   
   private final double maxValue_;
   private final double minValue_;


   /**
    * Simples constructor.
    *
    * @param value Value for the spinner.
    * @param minValue Minimum allowed value
    * @param maxValue Maximum allowed value
    */
   public FpsSpinnerNumberModel(double value, double minValue, double maxValue) {
      super(value, minValue, maxValue, BIGSTEP);
      minValue_ = minValue;
      maxValue_ = maxValue;
   }
   
  
   @Override
   public Object getNextValue() {
      Number val = super.getNumber();
      val = (val.doubleValue() >= CUTOFF)
            ? val.doubleValue() + BIGSTEP : val.doubleValue() + SMALLSTEP;
      val = (val.doubleValue() > maxValue_) ? maxValue_ : val;
      return val;
   }

   @Override
   public Object getPreviousValue() {
      Number val = super.getNumber();
      val = (val.doubleValue() > CUTOFF)
            ? val.doubleValue() - BIGSTEP : val.doubleValue() - SMALLSTEP;
      val = (val.doubleValue() < minValue_) ? minValue_ : val;
      return val;
   }
   
   
}


// Copyright (C) 2016-2017 Open Imaging, Inc.
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

package org.micromanager.display.internal.animate;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.micromanager.data.Coords;
import org.micromanager.data.Coords.CoordsBuilder;
import org.micromanager.data.internal.DefaultCoords;

/**
 * Manages the position within a dataset displayed during animated playback.
 *
 * Keeps track of the current position of the animation, and computes the
 * position that should be displayed next.
 *
 * @author Mark A. Tsuchida
 */
public class DataCoordsAnimationState implements AnimationStateDelegate<Coords> {
   /**
    * Interface for the delegate supplying presence/absence of data.
    * All methods must be thread safe.
    */
   public interface CoordsProvider {
      List<String> getOrderedAxes();
      int getMaximumExtentOfAxis(String axis); // O(1) recommended
      boolean coordsExist(Coords c);
      Collection<String> getAnimatedAxes();
   }

   private final CoordsProvider delegate_;
   private Coords animationCoords_ = new DefaultCoords.Builder().build();

   public static DataCoordsAnimationState create(CoordsProvider delegate) {
      if (delegate == null) {
         throw new NullPointerException();
      }
      return new DataCoordsAnimationState(delegate);
   }

   private DataCoordsAnimationState(CoordsProvider delegate) {
      delegate_ = delegate;
   }

   @Override
   public synchronized Coords getAnimationPosition() {
      // In the slightly pathological case where the CoordsProvider now has
      // an axis that it previsouly didn't, we supply zeros for indices on
      // those new axes.
      animationCoords_ = getFullPosition(animationCoords_);
      return animationCoords_;
   }

   @Override
   public synchronized void setAnimationPosition(Coords position) {
      animationCoords_ = getFullPosition(position);
   }

   @Override
   public synchronized Coords advanceAnimationPosition(double frames) {
      return advanceAnimationPositionImpl(frames, true);
   }

   private Coords advanceAnimationPositionImpl(double frames,
         boolean skipNonExistent)
   {
      final Coords prevPos = getFullPosition(animationCoords_);
      final List<String> axes = delegate_.getOrderedAxes();
      final Collection<String> animatedAxes = delegate_.getAnimatedAxes();

      if (animatedAxes.isEmpty()) {
         return animationCoords_;
      }

      CoordsBuilder cb = new DefaultCoords.Builder();
      int framesToAdvance = (int) Math.round(frames);
      for (String axis : Lists.reverse(axes)) {
         int prevIndex = prevPos.getIndex(axis);
         if (!animatedAxes.contains(axis) || framesToAdvance == 0) {
            cb.index(axis, prevIndex);
            continue;
         }
         int axisLength = delegate_.getMaximumExtentOfAxis(axis) + 1;
         int unwrappedNewIndex = prevIndex + framesToAdvance;
         cb.index(axis, unwrappedNewIndex % axisLength);
         framesToAdvance = unwrappedNewIndex / axisLength;
      }
      animationCoords_ = cb.build();

      // Skip forward to first extant coords. But guard against the possibility
      // that we will never find
      if (skipNonExistent && !axes.isEmpty()) {
         Coords start = animationCoords_;
         while (!delegate_.coordsExist(animationCoords_)) {
            advanceAnimationPositionImpl(1.0, false);
            if (animationCoords_.equals(start)) {
               // All coords are nonexistent; revert to original position
               animationCoords_ = prevPos;
               break;
            }
         }
      }

      return animationCoords_;
   }

   @Override
   public synchronized Coords getFullPosition(Coords partialPosition) {
      // Fill in missing coords with known current position, or else zero.
      Set<String> currentAxes = new HashSet<String>(animationCoords_.getAxes());
      Set<String> givenAxes = new HashSet<String>(partialPosition.getAxes());
      CoordsBuilder cb = new DefaultCoords.Builder();
      for (String axis : delegate_.getOrderedAxes()) {
         if (givenAxes.contains(axis)) {
            cb.index(axis, partialPosition.getIndex(axis));
         }
         else if (currentAxes.contains(axis)) {
            cb.index(axis, animationCoords_.getIndex(axis));
         }
         else {
            cb.index(axis, 0);
         }
      }
      return cb.build();
   }
}
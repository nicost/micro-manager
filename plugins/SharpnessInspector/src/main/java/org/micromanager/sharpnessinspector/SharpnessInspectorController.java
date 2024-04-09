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

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import ij.gui.Roi;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.micromanager.Studio;
import org.micromanager.data.Coords;
import org.micromanager.data.DataProviderHasNewImageEvent;
import org.micromanager.data.Image;
import org.micromanager.data.internal.DefaultImage;
import org.micromanager.display.DataViewer;
import org.micromanager.display.DisplayWindow;
import org.micromanager.display.inspector.AbstractInspectorPanelController;
import org.micromanager.display.inspector.internal.panels.intensity.ImageStatsPublisher;
import org.micromanager.events.StagePositionChangedEvent;
import org.micromanager.imageprocessing.ImgSharpnessAnalysis;
import org.micromanager.internal.utils.MustCallOnEDT;
import org.micromanager.propertymap.MutablePropertyMapView;
import org.micromanager.sharpnessinspector.ui.SharpnessInspectorPanel;

/**
 *
 * @author Nick Anthony
 */
public class SharpnessInspectorController extends AbstractInspectorPanelController {
   // For some reason a whole new instance of this class is created each time we switch display
   // viewers. Having this variable static allows it's value to stay unchanged between instances.
   private static boolean expanded_ = false;
   private static final String METHOD_KEY = "sharpnessMethod";
   private final SharpnessInspectorPanel panel_;
   private DataViewer viewer_;
   private final Studio studio_;
   private boolean autoImageEvaluation_ = false;
   private final SharpnessEvaluator eval_ = new SharpnessEvaluator();
    
   private SharpnessInspectorController(Studio studio) {
      studio_ = studio;
      final MutablePropertyMapView settings = studio_.profile().getSettings(this.getClass());
      studio_.events().registerForEvents(this);
      panel_ = new SharpnessInspectorPanel(this);
      eval_.setMethod(settings.getStringAsEnum(METHOD_KEY, ImgSharpnessAnalysis.Method.class,
              ImgSharpnessAnalysis.Method.Redondo));
      panel_.setEvaluationMethod(eval_.getMethod());
      panel_.addPropertyChangeListener("evalMethod", (evt) -> {
         eval_.setMethod((ImgSharpnessAnalysis.Method) evt.getNewValue());
         settings.putEnumAsString(METHOD_KEY, eval_.getMethod());
      });
        
      panel_.addScanRequestedListener((evt) -> {
         SwingWorker worker = new SwingWorker() {
               @Override
               protected Object doInBackground() throws Exception {
                  SharpnessInspectorController.this.beginScan(evt.intervalUm(), evt.rangeUm());
                  return null;
               }
         };
         worker.execute();
      });
        
   }
    
   public static SharpnessInspectorController create(Studio studio) {
      return new SharpnessInspectorController(studio);
   }

   @Override
   public String getTitle() {
      return "Image Sharpness";
   }

   @Override
   public JPanel getPanel() {
      return panel_;
   }

   @Override
   @MustCallOnEDT
   public void attachDataViewer(DataViewer viewer) {
      Preconditions.checkNotNull(viewer);
      if (!(viewer instanceof ImageStatsPublisher)) {
         throw new IllegalArgumentException("Programming error");
      }
      detachDataViewer();
      viewer_ = viewer;
      viewer.registerForEvents(this);
      viewer.getDataProvider().registerForEvents(this);
   }

   @Override
   @MustCallOnEDT
   public void detachDataViewer() {
      if (viewer_ == null) {
         return;
      }
      viewer_.getDataProvider().unregisterForEvents(this);
      viewer_.unregisterForEvents(this);
      viewer_ = null;
   }

   @Override
   public boolean isVerticallyResizableByUser() {
      return true;
   }

   @Override
   public void setExpanded(boolean status) {
      expanded_ = status;
      // If the UI is collapsed there is no reason to process images.
      autoImageEvaluation_ = status;
   }

   @Override
   public boolean initiallyExpand() {
      return expanded_;
   }

   @Subscribe
    public void onNewImage(DataProviderHasNewImageEvent evt) {
      // This is fired because we register for the dataprovider events.
      // Happens each time a new image is available from the provider.
      if (!this.autoImageEvaluation_) {
         return;
      }
      final DefaultImage img = (DefaultImage) evt.getImage();
      Roi roi;
      try {
         roi = ((DisplayWindow) viewer_).getImagePlus().getRoi();
      // Sometimes when the display window is just getting initialized this occurs due
      // to a nullpointer in trying to get the ImagePlus
      } catch (RuntimeException rte) {
         return;
      }
      if (roi == null || !roi.isArea()) {
         this.panel_.setRoiSelected(false);
         return;
      }
      this.panel_.setRoiSelected(true);
      Rectangle r = roi.getBounds();
      if (r.width < 5 || r.height < 5) {
         return; //Rectangle must be larger than the kernel used to calculate gradient which is 1x3
      }
      double grad = eval_.evaluate(img, r);
      double z = img.getMetadata().getZPositionUm();
      this.panel_.setValue(z, System.currentTimeMillis(), grad);
   }

   //TODO Many z stages don't fire this. use polling instead
   @Subscribe
   public void onZPosChanged(StagePositionChangedEvent evt) {
      if (!studio_.core().getFocusDevice().equals(evt.getDeviceName())) {
         return; //Stage device names don't match. We only want to use the default focus device.
      }
      this.panel_.setZPos(evt.getPos());
   }

   private void beginScan(double intervalUm, double rangeUm) {
      this.panel_.clearData();
      this.panel_.setPlotMode(PlotMode.Z);
      this.autoImageEvaluation_ = false;

      if (studio_.live().getIsLiveModeOn()) {
         studio_.live().setLiveMode(false);
      }
        
      Roi roi = ((DisplayWindow) viewer_).getImagePlus().getRoi();
      Rectangle r;
      if (roi == null || !roi.isArea()) {
         r = new Rectangle(// use full image fov
                  ((DisplayWindow) viewer_).getImagePlus().getWidth(),
                  ((DisplayWindow) viewer_).getImagePlus().getHeight());
      } else {
         r = roi.getBounds();
         //Rectangle must be larger than the kernel used to calculate gradient which is 1x3
         if (r.width < 5) {
            r.setSize(5, r.height);
         }
         if (r.height < 5) {
            r.setSize(r.width, 5);
         }
      }
      try {
         long numSteps = Math.round(rangeUm / intervalUm);
         final double startingPos = studio_.core().getPosition();
         // Move down by half of the range so that the scan is centered at the starting point.
         studio_.core().setRelativePosition(-(rangeUm / 2.0));
         while (studio_.core().deviceBusy(studio_.core().getFocusDevice())) { // make sure we moved
            Thread.sleep(50);
         }
         for (int i = 0; i < numSteps; i++) {
            studio_.core().setRelativePosition(intervalUm);
            // make sure we moved
            while (studio_.core().deviceBusy(studio_.core().getFocusDevice())) {
               Thread.sleep(50);
            }

            Image img = studio_.live().snap(true).get(0);
            double sharpness = eval_.evaluate(img, r);

            double pos = studio_.core().getPosition();
            panel_.setValue(pos, System.currentTimeMillis(), sharpness);
         }
         studio_.core().setPosition(startingPos);
      } catch (Exception e) {
         studio_.logs().showError(e);
      } finally {
         this.autoImageEvaluation_ = true;
      }
   }

   public void evalZ() {
      this.panel_.clearData();
      this.panel_.setPlotMode(PlotMode.Z);
      this.autoImageEvaluation_ = false;

      Roi roi = ((DisplayWindow) viewer_).getImagePlus().getRoi();
      Rectangle r;
      if (roi == null || !roi.isArea()) {
         r = new Rectangle(// use full image fov
                 ((DisplayWindow) viewer_).getImagePlus().getWidth(),
                 ((DisplayWindow) viewer_).getImagePlus().getHeight());
      } else {
         r = roi.getBounds();
         //Rectangle must be larger than the kernel used to calculate gradient which is 1x3
         if (r.width < 5) {
            r.setSize(5, r.height);
         }
         if (r.height < 5) {
            r.setSize(r.width, 5);
         }
      }
      try {
         Image img = viewer_.getDisplayedImages().get(0);
         Coords.Builder baseCoords = img.getCoords().copyRemovingAxes(Coords.Z).copyBuilder();
         int numZ = viewer_.getDataProvider().getSummaryMetadata().getIntendedDimensions().getZ();
         boolean useZ = true;
         Double previousZ = null;
         for (int i = 0; i < numZ; i++) {
            Coords coords = baseCoords.z(i).build();
            Image zImg = viewer_.getDataProvider().getImage(coords);
            if (Objects.equals(zImg.getMetadata().getZPositionUm(), previousZ)) {
               useZ = false;
               break;
            } else {
               previousZ = zImg.getMetadata().getZPositionUm();
            }
         }
         int zAtMaxScore = 0;
         double maxScore = 0;
         List<AbstractMap.SimpleEntry<Double, Double>> scores = new ArrayList<>();
         for (int i = 0; i < numZ; i++) {
            Coords coords = baseCoords.z(i).build();
            Image zImg = viewer_.getDataProvider().getImage(coords);
            double sharpness = eval_.evaluate(zImg, r);
            double z = zImg.getMetadata().getZPositionUm();
            if (useZ) {
               //panel_.setValue(z, System.currentTimeMillis(), sharpness);
               scores.add(new AbstractMap.SimpleEntry<>(z, sharpness));
            } else {
               //panel_.setValue(i, System.currentTimeMillis(), sharpness);
               scores.add(new AbstractMap.SimpleEntry<>((double) i, sharpness));
            }
            if (i == 0) {
               maxScore = sharpness;
            } else if (sharpness > maxScore) {
               maxScore = sharpness;
               zAtMaxScore = i;
            }
         }
         for (AbstractMap.SimpleEntry<Double, Double> entry : scores) {
            panel_.setValue(entry.getKey(), System.currentTimeMillis(), entry.getValue());
         }
         double gaussMax = panel_.findGaussianMax();
         // panel_.fitZ();
         panel_.setMaxZLabel((int) Math.round(gaussMax));
      } catch (Exception e) {
         studio_.logs().showError(e);
      } finally {
         this.autoImageEvaluation_ = true;
      }
   }


   public  double evalImage() {
      Roi roi;
      try {
         roi = ((DisplayWindow) viewer_).getImagePlus().getRoi();
         // Sometimes when the display window is just getting initialized this occurs due
         // to a nullpointer in trying to get the ImagePlus
      } catch (RuntimeException rte) {
         return 0.0;
      }
      try {
         Image img = viewer_.getDisplayedImages().get(0);
         if (roi == null || !roi.isArea()) {
            roi = new Roi(0, 0, img.getWidth(), img.getHeight());
         }
         return eval_.evaluate(img, roi.getBounds());
      } catch (IOException ioe) {
         studio_.logs().showError(ioe);
      }
      return 0.0;
   }

   public static class RequestScanEvent extends ActionEvent {
      private final double interval;
      private final double range;

      public RequestScanEvent(Object source, double intervalUm, double rangeUm) {
         super(source, 0, "startScan");
         interval = intervalUm;
         range = rangeUm;
      }

      public double intervalUm() {
         return interval;
      }

      public double rangeUm() {
         return range;
      }
   }

   public interface RequestScanListener {
      void actionPerformed(RequestScanEvent evt);
   }
   
   public enum PlotMode {
      Time,
      Z
   }
   
}
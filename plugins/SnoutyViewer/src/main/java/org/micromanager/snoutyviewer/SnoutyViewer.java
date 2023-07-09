/*
 * LICENSE: This file is distributed under the BSD license. License text is
 * included with the source distribution.
 *
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.
 *
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
 */

package org.micromanager.snoutyviewer;

// Note that this code uses imports for MMStudio internal packages.
// Plugins should not access internal packages, to ensure modularity and
// maintainability. However, this plugin code is older than the current
// MMStudio API, so it still uses internal classes and interfaces. New code
// should not imitate this practice.
import static org.micromanager.data.internal.BufferTools.NATIVE_ORDER;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.cleargl.recorder.VideoRecorderInterface;
import clearvolume.transferf.TransferFunctions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import coremem.fragmented.FragmentedMemory;
import java.awt.Color;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.micromanager.LogManager;
import org.micromanager.Studio;
import org.micromanager.data.Coordinates;
import org.micromanager.data.Coords;
import org.micromanager.data.DataProvider;
import org.micromanager.data.DataProviderHasNewImageEvent;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.data.Metadata;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.data.internal.DefaultDatastoreClosingEvent;
import org.micromanager.data.internal.DefaultImage;
import org.micromanager.display.ChannelDisplaySettings;
import org.micromanager.display.ComponentDisplaySettings;
import org.micromanager.display.DataViewer;
import org.micromanager.display.DataViewerListener;
import org.micromanager.display.DisplaySettings;
import org.micromanager.display.DisplaySettings.ColorMode;
import org.micromanager.display.DisplayWindow;
import org.micromanager.display.inspector.internal.panels.intensity.ImageStatsPublisher;
import org.micromanager.display.internal.event.DataViewerWillCloseEvent;
import org.micromanager.display.internal.event.DefaultDisplaySettingsChangedEvent;
import org.micromanager.display.internal.imagestats.BoundsRectAndMask;
import org.micromanager.display.internal.imagestats.ImageStatsProcessor;
import org.micromanager.display.internal.imagestats.ImageStatsRequest;
import org.micromanager.display.internal.imagestats.ImagesAndStats;
import org.micromanager.display.internal.imagestats.IntegerComponentStats;
import org.micromanager.events.ShutdownCommencingEvent;
import org.micromanager.snoutyviewer.events.CanvasDrawCompleteEvent;


/**
 * Micro-Manager DataViewer that shows 3D stack in the ClearVolume 3D Renderer.
 *
 * @author nico
 */
public class SnoutyViewer implements DataViewer, ImageStatsPublisher {

   private DisplaySettings displaySettings_;
   private final ImageStatsProcessor imageStatsProcessor_;
   private final Studio studio_;
   private DataProvider dataProvider_;
   private DataViewer clonedDisplay_;
   private ClearVolumeRendererInterface clearVolumeRenderer_;
   private String name_;
   private final EventBus displayBus_;
   private int maxValue_;
   private int activeChannel_ = 0;
   private Coords lastDisplayedCoords_;
   private ImagesAndStats lastCalculatedImagesAndStats_;
   private final Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA,
                                   Color.PINK, Color.CYAN, Color.YELLOW, Color.ORANGE};
   

   public SnoutyViewer(Studio studio) {
      this(studio, null);
   }

   /**
    * @param studio Entry point into everything Micro-Manager
    * @param provider Data to be displayed
    */
   public SnoutyViewer(final Studio studio, final DataProvider provider) {
      imageStatsProcessor_ = ImageStatsProcessor.create();

      studio_ = studio;
      if (provider == null) {
         clonedDisplay_ = studio_.displays().getActiveDataViewer();
         if (clonedDisplay_ != null) {
            dataProvider_ = clonedDisplay_.getDataProvider();
            name_ = clonedDisplay_.getName() + "-ClearVolume";
         }
      } else {
         dataProvider_ = provider;
         clonedDisplay_ = getDisplay(dataProvider_);
         if (clonedDisplay_ != null) {
            name_ = clonedDisplay_.getName() + "-ClearVolume";
         }
      }
      displayBus_ = new EventBus();

      if (dataProvider_ == null) {
         studio_.logs().showMessage("No data set open");
         return;
      }
      
      if (name_ == null) {
         name_ = dataProvider_.getSummaryMetadata().getPrefix();
      }

   }
   

   /**
    * Code that needs to register this instance with various managers and
    * listeners. Could have been in the constructor, except that it is unsafe to
    * register our instance before it is completed. Needs to be called right
    * after the constructor.
    */
   public void register() {

      dataProvider_.registerForEvents(this);
      studio_.displays().addViewer(this);
      studio_.events().registerForEvents(this);

      // used to reference our instance within the listeners:
      final SnoutyViewer ourViewer = this;


   }

   private void cleanup() {
      studio_.getDisplayManager().removeViewer(this);
      studio_.events().post(DataViewerWillCloseEvent.create(this));
      dataProvider_.unregisterForEvents(this);
      studio_.events().unregisterForEvents(this);
      clearVolumeRenderer_.close();
      imageStatsProcessor_.shutdown();
   }

   private void setOneChannelVisible(int chToBeVisible) {
      for (int ch = 0; ch < dataProvider_.getNextIndex(Coords.CHANNEL); ch++) {
         boolean setVisible = ch == chToBeVisible;
         clearVolumeRenderer_.setLayerVisible(ch, setVisible);
      }
   }
   
   private void setAllChannelsVisible() {
      for (int ch = 0; ch < dataProvider_.getNextIndex(Coords.CHANNEL); ch++) {
         clearVolumeRenderer_.setLayerVisible(ch, true);
      }
   }

   /**
    * There was an update to the display settings, so update the display
    * of the image to reflect the change.  Only change variables that actually
    * changed
    *
    * @param ds New display settings 
    */
   @Override
   public void setDisplaySettings(DisplaySettings ds) {
      if (displaySettings_.getColorMode() != ds.getColorMode()) {
         if (ds.getColorMode() == DisplaySettings.ColorMode.COMPOSITE) {
            setAllChannelsVisible();
         } else {
            setOneChannelVisible(activeChannel_); // todo: get the channel selected in the slider
         }
      }
      for (int ch = 0; ch < dataProvider_.getNextIndex(Coords.CHANNEL); ch++) {
         if (displaySettings_.isChannelVisible(ch) != ds.isChannelVisible(ch)) {
            clearVolumeRenderer_.setLayerVisible(ch, ds.isChannelVisible(ch));
         }
         Color nc = ds.getChannelColor(ch);
         if (ds.getColorMode() == DisplaySettings.ColorMode.GRAYSCALE) {
            nc = Color.WHITE;
         }
         if (displaySettings_.getChannelColor(ch) != nc
               || displaySettings_.getColorMode() != ds.getColorMode()) {
            clearVolumeRenderer_.setTransferFunction(ch, TransferFunctions.getGradientForColor(nc));
         }

         // Autostretch if set
         if (!Objects.equals(ds.isAutostretchEnabled(),
                 displaySettings_.isAutostretchEnabled())
                 || !Objects.equals(ds.getAutoscaleIgnoredPercentile(),
                         displaySettings_.getAutoscaleIgnoredPercentile())) {
            if (ds.isAutostretchEnabled()) {
               try {
                  ds = autostretch(ds);
               } catch (IOException ioe) {
                  studio_.logs().logError(ioe);
               }
            }
         }

         ChannelDisplaySettings displayChannelSettings = displaySettings_.getChannelSettings(ch);
         ChannelDisplaySettings dsChannelSettings = ds.getChannelSettings(ch);

         if (displayChannelSettings.getComponentSettings(0).getScalingMaximum()
               != dsChannelSettings.getComponentSettings(0).getScalingMaximum()
               || displayChannelSettings.getComponentSettings(0).getScalingMinimum()
               != dsChannelSettings.getComponentSettings(0).getScalingMinimum()) {
            float min = (float) dsChannelSettings.getComponentSettings(0).getScalingMinimum()
                  / (float) maxValue_;
            float max = (float) dsChannelSettings.getComponentSettings(0).getScalingMaximum()
                  / (float) maxValue_;
            clearVolumeRenderer_.setTransferFunctionRange(ch, min, max);
         }
         
         if (displayChannelSettings.getComponentSettings(0).getScalingGamma()
               != dsChannelSettings.getComponentSettings(0).getScalingGamma()) {
            clearVolumeRenderer_.setGamma(ch, 
                    dsChannelSettings.getComponentSettings(0).getScalingGamma());
         }
         
      }
  
      
      // Update the Inspector window
      this.postEvent(DefaultDisplaySettingsChangedEvent.create(
              this, displaySettings_, ds));
      
      // replace our reference to the display settings with the new one
      displaySettings_ = ds;
   }

   @Override
   public DisplaySettings getDisplaySettings() {
      return displaySettings_;
   }

   @Override
   public void registerForEvents(Object o) {
      displayBus_.register(o);
   }

   @Override
   public void unregisterForEvents(Object o) {
      displayBus_.unregister(o);
   }
   
   public void postEvent(Object o) {
      displayBus_.post(o);
   }

   @Override
   @Deprecated
   public Datastore getDatastore() {
      if (dataProvider_ instanceof Datastore) {
         return (Datastore) dataProvider_;
      }
      return null;
   }
   
   @Override
   public DataProvider getDataProvider() {
      return dataProvider_;
   }

   @Override
   @Deprecated
   public void setDisplayedImageTo(Coords coords) {
      setDisplayPosition(coords);
   }


   /**
    * Supposed to return all images currently shown.
    * In reality only returns the middle image of each z-stack.
    * Not sure, but probably doing so to avoid calculating the
    * histogram over the whole stack.
    *
    * @return List with middle image of each z stack
    */
   @Override
   public List<Image> getDisplayedImages() throws IOException {
      List<Image> imageList = new ArrayList<>();
      final int nrZ = dataProvider_.getNextIndex(Coords.Z);
      final int nrCh = dataProvider_.getNextIndex(Coords.CHANNEL);
      for (int ch = 0; ch < nrCh; ch++) {
         /*
         // return the complete stack
         for (int i = 0; i < nrZ; i++) {
            coordsBuilder_ = coordsBuilder_.z(i).channel(ch).time(0).stagePosition(0);
            Coords coords = coordsBuilder_.build();
            imageList.add(dataProvider_.getImage(coords));
         }
         */

         // Only return the middle image
         
         Coords coords = Coordinates.builder().z(nrZ / 2).channel(ch).t(0)
               .stagePosition(0).build();
         imageList.add(dataProvider_.getImage(coords));

      }
      return imageList;
   }


   @Override
   public String getName() {
      return name_;
   }

   /**
    * Draws the volume of the given time point in the viewer.
    *
    * @param timePoint zero-based index in the time axis
    * @param position zero-based index into the position axis
    * @throws IOException Possible with disk-back Dataproviders
    */
   public final void drawVolume(final int timePoint, final int position) throws IOException {
      //if (timePoint == currentlyShownTimePoint_)
      //   return; // nothing to do, already showing requested timepoint
      // create fragmented memory for each stack that needs sending to CV:
      Image randomImage = dataProvider_.getAnyImage();
      final Metadata metadata = randomImage.getMetadata();
      final SummaryMetadata summary = dataProvider_.getSummaryMetadata();
      final int nrZ = dataProvider_.getNextIndex(Coords.Z);
      final int nrCh = dataProvider_.getNextIndex(Coords.CHANNEL);

      clearVolumeRenderer_.setVolumeDataUpdateAllowed(false);

      for (int ch = 0; ch < nrCh; ch++) {
         FragmentedMemory fragmentedMemory = new FragmentedMemory();
         for (int i = 0; i < nrZ; i++) {
            Coords coords = Coordinates.builder().z(i).channel(ch).t(timePoint)
                  .stagePosition(position).build();
            lastDisplayedCoords_ = coords;

            // Bypass Micro-Manager api to get access to the pixels to avoid extra copying
            DefaultImage image = (DefaultImage) dataProvider_.getImage(coords);
            int maxIntensity = 255;

            // add the contiguous memory as fragment:
            if (image != null) {
               if (image.getBytesPerPixel() == 1) {
                  byte[] pixels = ((ByteBuffer) image.getPixelBuffer()).array();
                  fragmentedMemory.add(ByteBuffer.allocateDirect(pixels.length).put(pixels));
               } else if (image.getBytesPerPixel() == 2) {
                  maxIntensity = 65535;
                  short[] pixels = ((ShortBuffer) image.getPixelBuffer()).array();
                  fragmentedMemory.add(ByteBuffer.allocateDirect(
                        2 * pixels.length).order(NATIVE_ORDER).asShortBuffer().put(pixels));
               }
            } else {
               // if the image is missing, replace with pixels initialized to 0
               fragmentedMemory.add(ByteBuffer.allocateDirect(
                        randomImage.getHeight()
                              * randomImage.getWidth()
                              * randomImage.getBytesPerPixel()));
            }
            maxValue_ = maxIntensity;
         }

         // TODO: correct x and y voxel sizes using aspect ratio
         double pixelSizeUm = metadata.getPixelSizeUm();
         if (pixelSizeUm == 0.0) {
            pixelSizeUm = 1.0;
         }
         Double stepSizeUm = summary.getZStepUm();
         if (stepSizeUm == null || stepSizeUm == 0.0) {
            stepSizeUm = 1.0;
         }
         
         // pass data to renderer: (this call takes a long time!)
         clearVolumeRenderer_.setVolumeDataBuffer(0, 
                 TimeUnit.SECONDS, 
                 ch,
                 fragmentedMemory,
                 randomImage.getWidth(),
                 randomImage.getHeight(),
                 nrZ, 
                 pixelSizeUm,
                 pixelSizeUm, 
                 stepSizeUm);


         // Set various display options:
         // HACK: on occasion we get null colors, correct that problem here
         Color chColor = displaySettings_.getChannelColor(ch);
         if (chColor == null) {
            chColor = colors[ch];
            List<Color> chColors = displaySettings_.getAllChannelColors();
            chColors.add(nrCh, chColor);
         }
         if (displaySettings_.getColorMode() == ColorMode.GRAYSCALE) {
            chColor = Color.WHITE;
         }
         clearVolumeRenderer_.setLayerVisible(ch, displaySettings_.isChannelVisible(ch));
         clearVolumeRenderer_.setTransferFunction(ch,
                 TransferFunctions.getGradientForColor(chColor));
         ChannelDisplaySettings cd = displaySettings_.getChannelSettings(ch);
         try {
            float max = (float) cd.getComponentSettings(0).getScalingMaximum()
                    / (float) maxValue_;
            float min = (float) cd.getComponentSettings(0).getScalingMinimum()
                    / (float) maxValue_;
            clearVolumeRenderer_.setTransferFunctionRange(ch, min, max);
            double contrastGamma = cd.getComponentSettings(0).getScalingGamma();
            clearVolumeRenderer_.setGamma(ch, contrastGamma);
         } catch (NullPointerException ex) {
            studio_.logs().logError(ex);
         }
      }
      clearVolumeRenderer_.setVolumeDataUpdateAllowed(true);
      
      // This call used to time out, now appears to work      
      if (!clearVolumeRenderer_.waitToFinishAllDataBufferCopy(2, TimeUnit.SECONDS)) {
         studio_.logs().logError("ClearVolume timed out after 2 seconds");
      }

      setDisplaySettings(displaySettings_);
  
   }

   /*
    * Series of functions that are merely pass through to the underlying 
    * clearVolumeRenderer
   */
   
   /**
    * I would have liked an on/off control here, but the ClearVolume api
    * only has a toggle function.
    */
   public void toggleWireFrameBox() {
      if (clearVolumeRenderer_ != null) {
         clearVolumeRenderer_.toggleBoxDisplay();
      }
   }
   
   public void toggleControlPanelDisplay() {
      if (clearVolumeRenderer_ != null) {
         clearVolumeRenderer_.toggleControlPanelDisplay();
      }
   }
   
   public void toggleParametersListFrame() {
      if (clearVolumeRenderer_ != null) {
         clearVolumeRenderer_.toggleParametersListFrame();
      }
   }

   /**
    * Implements the Reset button in the display.  Resets translation and rotation to where they
    * were at the start.
    */
   public void resetRotationTranslation() {
      if (clearVolumeRenderer_ != null) {
         clearVolumeRenderer_.resetRotationTranslation();
         float x = clearVolumeRenderer_.getTranslationX();
         float y = clearVolumeRenderer_.getTranslationY();
         float z = clearVolumeRenderer_.getTranslationZ();
         studio_.logs().logMessage("Rotation now is: " + x + ", " + y + ", " + z);
      }
   }
   
   /**
    * Centers the visible part of the ClipBox.
    * It seems that 0, 0 is in the center of the full volume, and
    * that -1 and 1 are at the edges of the volume
    */
   public void center() {
      if (clearVolumeRenderer_ != null) {
         float[] clipBox = clearVolumeRenderer_.getClipBox();
         clearVolumeRenderer_.setTranslationX(-(clipBox[1] + clipBox[0]) / 2.0f);
         clearVolumeRenderer_.setTranslationY(-(clipBox[3] + clipBox[2]) / 2.0f);
         // do not change TranslationZ, since that mainly changes how close we are
         // to the object, not really the rotation point
         // clearVolumeRenderer_.setTranslationZ( -5);
      }
   }
   
   /**
    * Resets the rotation so that the object lines up with the xyz axis.
    */
   public void straighten() {
      if (clearVolumeRenderer_ != null) {
         // Convoluted way to reset the rotation
         // I probably should use rotationControllers...
         float x = clearVolumeRenderer_.getTranslationX();
         float y = clearVolumeRenderer_.getTranslationY();
         float z = clearVolumeRenderer_.getTranslationZ();
         clearVolumeRenderer_.resetRotationTranslation();
         clearVolumeRenderer_.setTranslationX(x);
         clearVolumeRenderer_.setTranslationY(y);
         clearVolumeRenderer_.setTranslationZ(z);
      }
   }

   /**
    * Attaches a video recorder to the current viewer.
    *
    * @param recorder VideoRecorder to be attched.
    */
   public void attachRecorder(VideoRecorderInterface recorder) {
      Runnable dt = new Thread(() -> {
         clearVolumeRenderer_.setVideoRecorder(recorder);
         clearVolumeRenderer_.toggleRecording();
         // Force an update of the display to start the recording immediately
         clearVolumeRenderer_.addTranslationZ(0.0);
      });
      SwingUtilities.invokeLater(dt);
   }
   
   public void toggleRecording() {
      Runnable dt = new Thread(() -> clearVolumeRenderer_.toggleRecording());
      SwingUtilities.invokeLater(dt);
   }


   /**
    * Find the first DisplayWindow attached to this dataprovider.
    *
    * @param provider first DisplayWindow or null if not found
    */
   private DisplayWindow getDisplay(DataProvider provider) {
      List<DisplayWindow> dataWindows = studio_.displays().getAllImageWindows();
      for (DisplayWindow dv : dataWindows) {
         if (provider == dv.getDataProvider()) {
            return dv;
         }
      }
      return null;
   }
   
   private DisplaySettings autostretch(DisplaySettings displaySettings) throws IOException {
      if (lastCalculatedImagesAndStats_ == null) {
         return displaySettings;
      }
          
      DisplaySettings.Builder newSettingsBuilder = displaySettings.copyBuilder();
      Coords baseCoords = getDisplayedImages().get(0).getCoords();
      double extremaPercentage = displaySettings.getAutoscaleIgnoredPercentile();
      if (extremaPercentage < 0.0) {
         extremaPercentage = 0.0;
      }
      for (int ch = 0; ch < dataProvider_.getNextIndex(Coords.CHANNEL); ++ch) {
         Image image = dataProvider_.getImage(baseCoords.copyBuilder().channel(ch).build());
         if (image != null) {
            ChannelDisplaySettings.Builder csCopyBuilder = 
                    displaySettings.getChannelSettings(ch).copyBuilder();
            for (int j = 0; j < image.getNumComponents(); ++j) {
               IntegerComponentStats componentStats = 
                       lastCalculatedImagesAndStats_.getResult().get(ch).getComponentStats(0);
               ComponentDisplaySettings.Builder ccB =
                     csCopyBuilder.getComponentSettings(j).copyBuilder();
               ccB.scalingRange(componentStats.getAutoscaleMinForQuantile(extremaPercentage),
                       componentStats.getAutoscaleMaxForQuantile(extremaPercentage));
               ccB.scalingGamma(displaySettings.getChannelSettings(ch)
                     .getComponentSettings(j).getScalingGamma());
               csCopyBuilder.component(j, ccB.build());
            }
            newSettingsBuilder.channel(ch, csCopyBuilder.build());
         }
      }
      DisplaySettings newSettings = newSettingsBuilder.build();
      postEvent(DefaultDisplaySettingsChangedEvent.create(this, displaySettings, 
              newSettings));
      
      return newSettings;
   }

   /**
    * Application message bus handler called when the application is shutting down.
    *
    * @param sce The ShutdownCommencing event itself.
    */
   @Subscribe
   public void onShutdownCommencing(ShutdownCommencingEvent sce) {
      cleanup();
   }

   @Subscribe
   public void onDataProviderHasNewImage(DataProviderHasNewImageEvent newImage) {
      if (dataProvider_ != newImage.getDataProvider()) {
         return;
      }
      Coords newImageCoords = newImage.getCoords();
      if (timePointComplete(newImageCoords.getT(), dataProvider_, studio_.logs())) {
         studio_.logs().logMessage("Do something");
      }
   }       
   
   @Subscribe
   public void onDataStoreClosingEvent(DefaultDatastoreClosingEvent ddce) {
   }
   
   /**
    * Check if we have all z slices for all channels at the given time point.
    * This code may be fooled by other axes in the data.
    *
    * @param timePointIndex - time point index
    * @param dataProvider - data that are being displayed
    * @param logger - Instance of the LogManager to log errors
    * @return true if complete, false otherwise
    */
   public static boolean timePointComplete(final int timePointIndex,
           final DataProvider dataProvider, final LogManager logger) {
      Coords zStackCoords = Coordinates.builder().t(timePointIndex).build();
      try {
         final int nrImages = dataProvider.getImagesMatching(zStackCoords).size();
         Coords intendedDimensions = dataProvider.getSummaryMetadata()
               .getIntendedDimensions();
         return nrImages >= intendedDimensions.getChannel() * intendedDimensions.getZ(); 
      } catch (IOException ioe) {
         logger.showError(ioe, "Error getting number of images from dataset");
      }
      return false;
   }

   @Override
   public boolean compareAndSetDisplaySettings(DisplaySettings originalSettings,
                                               DisplaySettings newSettings) {
      if (newSettings == null) {
         throw new NullPointerException("Display settings must not be null");
      }
      synchronized (this) {
         if (originalSettings != displaySettings_) {
            // We could compare the contents, but it's probably not worth the
            // effort; spurious failures should not affect proper usage
            return false;
         }
         if (newSettings == displaySettings_) {
            return true;
         }
         setDisplaySettings(newSettings);
         return true;
      }
   }

   @Override
   public void setDisplayPosition(Coords position, boolean forceRedisplay) {
      if (forceRedisplay || !position.equals(lastDisplayedCoords_)) {
         setDisplayPosition(position);
      }
   }

   @Override
   public void setDisplayPosition(Coords coords) {
      // ClearVolume uses commands and keystrokes that work on a given channel
      // Make sure that the channel that ClearVolume works on is synced with the
      // Channel slider position in the ClearVolume panel in the Image Inspector
      activeChannel_ = coords.getChannel();
      if (displaySettings_.getColorMode() != DisplaySettings.ColorMode.COMPOSITE) {
         setOneChannelVisible(coords.getChannel());
      }
      clearVolumeRenderer_.setCurrentRenderLayer(coords.getChannel());
      try {
         drawVolume(coords.getT(), coords.getP());
      } catch (IOException ioe) {
         studio_.logs().logError(ioe);
      }
      lastDisplayedCoords_ = coords;
      displayBus_.post(new CanvasDrawCompleteEvent());
   }

   @Override
   public Coords getDisplayPosition() {
      return lastDisplayedCoords_;
   }

   @Override
   public boolean compareAndSetDisplayPosition(Coords originalPosition, 
           Coords newPosition, boolean forceRedisplay) {
      boolean display = originalPosition != newPosition;
      if (display || forceRedisplay) {
         setDisplayPosition(newPosition);
      }
      return display;
   }

   @Override
   public boolean compareAndSetDisplayPosition(Coords originalPosition, Coords newPosition) {
      return compareAndSetDisplayPosition(originalPosition, newPosition, false);
   }

   @Override
   public boolean isVisible() {
      return clearVolumeRenderer_ != null && clearVolumeRenderer_.isShowing();
   }

   @Override
   public boolean isClosed() {
      return clearVolumeRenderer_ == null;
   }

   @Override
   public void addListener(DataViewerListener listener, int priority) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public void removeListener(DataViewerListener listener) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public ImagesAndStats getCurrentImagesAndStats() {
                  
      if (lastDisplayedCoords_ == null) {
         return null;
      }
      // Only compute the statistic for the middle coordinates, otherwise the 
      // computation is too slow
      // TODO: figure out how to compute the whole histogram in the background
      int middleSlice = dataProvider_.getNextIndex(Coords.Z) / 2;
      
      Coords position = lastDisplayedCoords_.copyBuilder().z(middleSlice).build();
      
      // Always compute stats for all channels
      List<Image> images;
      try {
         images = dataProvider_.getImagesIgnoringAxes(position, Coords.C);
      } catch (IOException e) {
         // TODO Should display error
         images = Collections.emptyList();
      }
      
      if (images == null) {
         return null;
      }

      // Images are sorted by channel here, since we don't (yet) have any other
      // way to correctly recombine stats with newer images (when update rate
      // is finite).
      if (images.size() > 1) {
         Collections.sort(images, (Image o1, Image o2) -> new Integer(o1.getCoords().getChannel())
               .compareTo(o2.getCoords().getChannel()));
      }
      
      BoundsRectAndMask selection = BoundsRectAndMask.unselected();

      ImageStatsRequest request = ImageStatsRequest.create(position, images, selection);

      ImagesAndStats process = null;
      
      try {
         process = imageStatsProcessor_.process(1, request, true);
      } catch (InterruptedException ex) {
         //Logger.getLogger(CVViewer.class.getName()).log(Level.SEVERE, null, ex);
      }
      
      lastCalculatedImagesAndStats_ = process;
      
      return process;
   }

}

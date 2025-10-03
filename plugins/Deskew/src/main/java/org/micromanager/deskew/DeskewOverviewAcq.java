package org.micromanager.deskew;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import mmcorej.org.json.JSONObject;
import org.micromanager.PositionList;
import org.micromanager.Studio;
import org.micromanager.acqj.main.Acquisition;
import org.micromanager.acqj.main.AcquisitionEvent;
import org.micromanager.acqj.util.AcquisitionEventIterator;
import org.micromanager.acquisition.ChannelSpec;
import org.micromanager.acquisition.SequenceSettings;
import org.micromanager.acquisition.internal.AcquisitionEngine;
import org.micromanager.acquisition.internal.DefaultAcquisitionStartedEvent;
import org.micromanager.acquisition.internal.MMAcquisition;
import org.micromanager.acquisition.internal.acqengjcompat.AcqEngJAdapter;
import org.micromanager.acquisition.internal.acqengjcompat.MDAAcqEventModules;
import org.micromanager.data.Datastore;
import org.micromanager.data.Pipeline;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.data.internal.DefaultSummaryMetadata;
import org.micromanager.internal.MMStudio;
import org.micromanager.internal.propertymap.NonPropertyMapJSONFormats;
import org.micromanager.internal.utils.AcqOrderMode;

public class DeskewOverviewAcq {
   Studio studio_;
   int rows_;
   int columns_;
   AcquisitionEngine engine_;

   public DeskewOverviewAcq(Studio studio, int rows, int columns) {
      studio_ = studio;
      rows_ = rows;
      columns_ = columns;
      engine_ = ((MMStudio) studio_).getAcquisitionEngine();
   }

   public void run() {
      final SequenceSettings acqSettings = studio_.acquisitions().getAcquisitionSettings()
               .copyBuilder()
               .useFrames(false)
               .useAutofocus(false)
               .usePositionList(false)
               .shouldDisplayImages(true)
               .build();


      if (engine_ == null || !(engine_ instanceof AcqEngJAdapter)) {
         studio_.logs().showError("Acquisition engine is not compatible. Use the new engine.");
         return;
      }
      DeskewOverViewAcqDataSink sink = new DeskewOverViewAcqDataSink(studio_.events(), acqSettings,
               this);
      Acquisition currentAcquisition = new Acquisition(sink);
      currentAcquisition.setDebugMode(studio_.core().debugLogEnabled());

      try {
         JSONObject summaryMetadataJSON = currentAcquisition.getSummaryMetadata();
         SummaryMetadata summaryMetadata = DefaultSummaryMetadata.fromPropertyMap(
                  NonPropertyMapJSONFormats.summaryMetadata().fromJSON(
                           summaryMetadataJSON.toString()));
         SummaryMetadata sm = summaryMetadata.copyBuilder().sequenceSettings(acqSettings).build();
         MMAcquisition acq = new MMAcquisition(studio_, sm, null, acqSettings);
         Datastore curStore = acq.getDatastore();
         Pipeline curPipeline = acq.getPipeline();
         sink.setDatastore(curStore);
         sink.setPipeline(curPipeline);

         // zStage_ = core_.getFocusDevice();

         studio_.events().registerForEvents(this);
         studio_.events().post(new DefaultAcquisitionStartedEvent(curStore, this, acqSettings));


         currentAcquisition.start();

         // Start the events and signal to finish when complete
         currentAcquisition.submitEventIterator(createAcqEventIterator(acqSettings,
                  null,
                  currentAcquisition));
         currentAcquisition.finish();
      } catch (Exception e) {
         studio_.logs().showError(e, "Failed to start acquisition");
         return;
      }


   }

   /**
    * This function converts acquisitionSettings to a lazy sequence (i.e. an iterator) of
    * AcquisitionEvents.
    */
   private Iterator<AcquisitionEvent> createAcqEventIterator(SequenceSettings acquisitionSettings,
                                                             PositionList posList,
                                                             Acquisition currentAcquisition)
            throws Exception {
      // Select channels that we are actually using
      List<ChannelSpec> chSpecs = new ArrayList<>();
      for (ChannelSpec chSpec : acquisitionSettings.channels()) {
         if (chSpec.useChannel()) {
            chSpecs.add(chSpec);
         }
      }

      Function<AcquisitionEvent, Iterator<AcquisitionEvent>> zStack = null;
      if (acquisitionSettings.useSlices()) {
         double origin = acquisitionSettings.slices().get(0);
         if (acquisitionSettings.relativeZSlice()) {
            origin = studio_.core().getPosition() + acquisitionSettings.slices().get(0);
         }
         zStack = MDAAcqEventModules.zStack(0,
                  acquisitionSettings.slices().size() - 1,
                  acquisitionSettings.sliceZStepUm(),
                  origin,
                  chSpecs,
                  null);
      } else if (acquisitionSettings.useChannels() && !chSpecs.isEmpty()) {
         boolean hasZOffsets = chSpecs.stream().anyMatch(t -> t.zOffset() != 0);
         if (hasZOffsets) {
            // add a fake z stack so that the channel z-offsets are handles correctly
            zStack = MDAAcqEventModules.zStack(0,
                     0,
                     0.1,
                     studio_.core().getPosition(),
                     chSpecs,
                     null);
         }
      }

      Function<AcquisitionEvent, Iterator<AcquisitionEvent>> channels = null;
      if (acquisitionSettings.useChannels()) {
         if (chSpecs.size() > 0) {
            Integer middleSliceIndex = (acquisitionSettings.slices().size() - 1) / 2;
            channels = MDAAcqEventModules.channels(chSpecs, middleSliceIndex, null);
         }
      }

      Function<AcquisitionEvent, Iterator<AcquisitionEvent>> positions = null;
      if (acquisitionSettings.usePositionList()) {
         positions = MDAAcqEventModules.positions(posList, null, studio_.core());
         // TODO: is acq engine supposed to move multiple stages?
         // Yes: when moving to a new position, all stages in the MultiStagePosition instance
         // should be moved to the desired location
         // TODO: What about Z positions in position list
         // Yes: First move all stages in the MSP to their desired location, then do
         // whatever is asked to do.
      }

      Function<AcquisitionEvent, Iterator<AcquisitionEvent>> timelapse = null;
      if (acquisitionSettings.useFrames()) {
         timelapse = MDAAcqEventModules.timelapse(acquisitionSettings.numFrames(),
                  acquisitionSettings.intervalMs(), null);
         // TODO custom time intervals
      }

      ArrayList<Function<AcquisitionEvent, Iterator<AcquisitionEvent>>> acqFunctions
               = new ArrayList<>();
      if (acquisitionSettings.acqOrderMode() == AcqOrderMode.POS_TIME_CHANNEL_SLICE) {
         if (acquisitionSettings.usePositionList()) {
            acqFunctions.add(positions);
         }
         if (acquisitionSettings.useFrames()) {
            acqFunctions.add(timelapse);
         }
         if (acquisitionSettings.useChannels()) {
            acqFunctions.add(channels);
         }
         if (zStack != null) {
            acqFunctions.add(zStack);
         }
      } else if (acquisitionSettings.acqOrderMode() == AcqOrderMode.POS_TIME_SLICE_CHANNEL) {
         if (acquisitionSettings.usePositionList()) {
            acqFunctions.add(positions);
         }
         if (acquisitionSettings.useFrames()) {
            acqFunctions.add(timelapse);
         }
         if (zStack != null) {
            acqFunctions.add(zStack);
         }
         if (acquisitionSettings.useChannels()) {
            acqFunctions.add(channels);
         }
      } else if (acquisitionSettings.acqOrderMode() == AcqOrderMode.TIME_POS_CHANNEL_SLICE) {
         if (acquisitionSettings.useFrames()) {
            acqFunctions.add(timelapse);
         }
         if (acquisitionSettings.usePositionList()) {
            acqFunctions.add(positions);
         }
         if (acquisitionSettings.useChannels()) {
            acqFunctions.add(channels);
         }
         if (zStack != null) {
            acqFunctions.add(zStack);
         }
      } else if (acquisitionSettings.acqOrderMode() == AcqOrderMode.TIME_POS_SLICE_CHANNEL) {
         if (acquisitionSettings.useFrames()) {
            acqFunctions.add(timelapse);
         }
         if (acquisitionSettings.usePositionList()) {
            acqFunctions.add(positions);
         }
         if (zStack != null) {
            acqFunctions.add(zStack);
         }
         if (acquisitionSettings.useChannels()) {
            acqFunctions.add(channels);
         }
      } else {
         throw new RuntimeException("Unknown acquisition order");
      }

      AcquisitionEvent baseEvent = new AcquisitionEvent(currentAcquisition);
      return new AcquisitionEventIterator(baseEvent, acqFunctions,
               acqEventMonitor(acquisitionSettings));

   }

   protected Function<AcquisitionEvent, AcquisitionEvent> acqEventMonitor(
            SequenceSettings acquisitionSettings) {
      return null;
   }

   public boolean abortRequest() {
      if (engine_ != null) {
         return engine_.abortRequest();
      }
      return true;
   }

}

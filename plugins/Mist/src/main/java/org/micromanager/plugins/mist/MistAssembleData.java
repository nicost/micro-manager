package org.micromanager.plugins.mist;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import org.micromanager.MultiStagePosition;
import org.micromanager.Studio;
import org.micromanager.alerts.UpdatableAlert;
import org.micromanager.data.Coords;
import org.micromanager.data.DataProvider;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.data.internal.DefaultImageJConverter;
import org.micromanager.display.DataViewer;

public class MistAssembleData {

   private enum PositionConvention {
      NotFound,   // Convention could not be determines
      HCS,        // Created by HCS plugin
      Magellan,   // Created by Magellan plugin
      Grid        // Created by Create Grid in StagePositionList
   }

   /**
    * This function can take a long, long time to execute.  Make sure not to call it on the EDT.
    *
    * @param locationsFile Output file from the Mist stitching plugin.  "img-global-positions-0"
    * @param dataViewer Micro-Manager dataViewer containing the input data/
    * @param newStore Datastore to write the stitched images to.
    */
   public static void assembleData(Studio studio,
                                   JButton button,
                                   String locationsFile,
                                   final DataViewer dataViewer,
                                   Datastore newStore,
                                   List<String> channelList,
                                   Map<String, Integer> mins,
                                   Map<String, Integer> maxes) {
      List<MistGlobalData> mistEntries = new ArrayList<>();
      PositionConvention positionConvention = PositionConvention.NotFound;

      File mistFile = new File(locationsFile);
      if (!mistFile.exists()) {
         studio.logs().showError("Mist global positions file not found: "
               + mistFile.getAbsolutePath());
         return;
      }
      if (dataViewer == null) {
         studio.logs().showError("No Micro-Manager data set selected");
         return;
      }
      DataProvider dp = dataViewer.getDataProvider();
      List<MultiStagePosition> stagePositionList = dp.getSummaryMetadata().getStagePositionList();

      UpdatableAlert updatableAlert = studio.alerts().postUpdatableAlert("Mist",
            "Started processing");
      try {
         // parse global position file into MistGlobalData objects
         BufferedReader br
               = new BufferedReader(new FileReader(mistFile));
         String line;
         int siteNr = -1;
         while ((line = br.readLine()) != null) {
            if (!line.startsWith("file: ")) {
               continue;
            }
            // interpreting the file name is complicated, since it relies on naming of the
            // Multistageposition, and multiple different strategies are possible.
            // - The HCS plugin names sites: B7-Site_0 B7-Site_1 C7-Site_0 C7-Site_1
            // - The Magellen plugin names sites: Grid_0_0 Grid_1_0 Grid_0_1 Grid_1_1
            //   where it is ambiguous if this is row_column or column row
            // - The Create Grid Utility in the StagePositionlist names sites:
            //       Pos-4-000_000 Pos-4-000_001 Pos-4-001_000
            //       where the first number appears to be the row and second the column
            // This plugin was originally written for the HCS plugin and would deduce
            // the site numbers from that convention. Try to first deduce which of the three
            // (if at all) conventions are used.

            int fileNameEnd = line.indexOf(';');
            String fileName = line.substring(6, fileNameEnd);
            if (positionConvention == PositionConvention.NotFound) {
               // HCS: looks for the word "Site"
               int lastUnderscore = fileName.lastIndexOf('_');
               if (fileName.substring(lastUnderscore - 4, lastUnderscore).equals("Site"))  {
                  positionConvention = PositionConvention.HCS;
               } else {
                  int secondLastUnderscore = fileName.substring(0, lastUnderscore - 1)
                        .lastIndexOf('_');
                  if (fileName.substring(secondLastUnderscore - 4, secondLastUnderscore)
                        .equals("Grid")) {
                     positionConvention = PositionConvention.Magellan;
                  } else {
                     int lastDash = fileName.lastIndexOf('-');
                     int secondLastDash = fileName.lastIndexOf('-', lastDash - 1);
                     if (secondLastDash != -1 && fileName.substring(
                           secondLastDash - 3, secondLastDash).equals("Pos")) {
                        positionConvention = PositionConvention.Grid;
                     }
                  }
               }
            }
            if (PositionConvention.NotFound.equals(positionConvention)) {
               updatableAlert.setText("Failed to parse Miss file");
               studio.logs().showError("Filenames in Mist file could not be parsed correctly");
               return;
            }
            String well = "";
            int index = fileName.indexOf("MMStack_");
            int end = fileName.substring(index).indexOf("-") + index;
            // x, y
            int posStart = line.indexOf("position: ") + 11;
            String lineEnd = line.substring(posStart);
            String xy = lineEnd.substring(0, lineEnd.indexOf(')'));
            String[] xySplit = xy.split(",");
            int positionX = Integer.parseInt(xySplit[0]);
            int positionY = Integer.parseInt(xySplit[1].trim());
            // row, column
            int gridStart = line.indexOf("grid: ") + 7;
            lineEnd = line.substring(gridStart);
            String rowCol = lineEnd.substring(0, lineEnd.indexOf(')'));
            String[] rowColSplit = rowCol.split(",");
            int row = Integer.parseInt(rowColSplit[0]);
            int col = Integer.parseInt(rowColSplit[1].trim());
            if (PositionConvention.HCS.equals(positionConvention)) {
               siteNr = Integer.parseInt(fileName.substring(fileName.lastIndexOf('_') + 1,
                     fileName.length() - 8));
               well = fileName.substring(index + 8, end);
            } else { // neither the Grid or Magellan convention have wells or SiteNrs
               if (PositionConvention.Magellan.equals(positionConvention)) {
                  String position = fileName.substring(index + 8, fileNameEnd - 14);
                  boolean found = false;
                  for (int i = 0; i < stagePositionList.size() && !found; i++) {
                     if (stagePositionList.get(i).getLabel().equals(position)) {
                        siteNr = i;
                        found = true;
                     }
                  }
               } else {
                  // TODO
                  siteNr++;
               }
            }
            mistEntries.add(new MistGlobalData(
                  fileName, siteNr, well, positionX, positionY, row, col));
         }
      } catch (IOException e) {
         studio.logs().showError("Error reading Mist global positions file: " + e.getMessage());
         return;
      } catch (NumberFormatException e) {
         studio.logs().showError("Error parsing Mist global positions file: " + e.getMessage());
         return;
      }

      SwingUtilities.invokeLater(() -> button.setEnabled(false));

      // calculate new image dimensions
      int imWidth = dp.getSummaryMetadata().getImageWidth();
      int imHeight = dp.getSummaryMetadata().getImageHeight();
      int maxX = 0;
      int maxY = 0;
      for (MistGlobalData entry : mistEntries) {
         if (entry.getPositionX() > maxX) {
            maxX = entry.getPositionX();
         }
         if (entry.getPositionY() > maxY) {
            maxY = entry.getPositionY();
         }
      }

      int newWidth = maxX + imWidth;
      int newHeight = maxY + imHeight;

      final int newNrC = channelList.size();
      final int newNrT = (maxes.getOrDefault(Coords.T, 0) - mins.getOrDefault(Coords.T, 0)
            + 1);
      final int newNrZ = (maxes.getOrDefault(Coords.Z, 0) - mins.getOrDefault(Coords.Z, 0)
            + 1);
      final int newNrP = dp.getSummaryMetadata().getIntendedDimensions().getP()
            / mistEntries.size();
      int maxNumImages = newNrC * newNrT * newNrZ * newNrP;
      ProgressMonitor monitor = new ProgressMonitor(this,
            "Stitching images...", null, 0, maxNumImages);
      DataViewer newDataViewer = null;
      long startTime = System.currentTimeMillis();
      try {
         // create datastore to hold the result
         Coords dims = dp.getSummaryMetadata().getIntendedDimensions();
         Coords.Builder cb = dims.copyBuilder().c(newNrC).t(newNrT).z(newNrZ).p(newNrP);
         newStore.setSummaryMetadata(dp.getSummaryMetadata().copyBuilder().imageHeight(newHeight)
               .imageWidth(newWidth).intendedDimensions(cb.build())
               .build());
         if (profileSettings_.getBoolean("shouldDisplay", true)) {
            newDataViewer = studio.displays().createDisplay(newStore);
         }
         Coords id = dp.getSummaryMetadata().getIntendedDimensions();
         Coords.Builder intendedDimensionsB = dp.getSummaryMetadata().getIntendedDimensions()
               .copyBuilder();
         for (String axis : new String[] {Coords.C, Coords.P, Coords.T, Coords.C}) {
            if (!id.hasAxis(axis)) {
               intendedDimensionsB.index(axis, 1);
            }
         }
         Coords intendedDimensions = intendedDimensionsB.build();
         Coords.Builder imgCb = studio.data().coordsBuilder();
         int nrImages = 0;
         for (int newP = 0; newP < newNrP; newP++) {
            int tmpC = -1;
            for (int c = 0; c < intendedDimensions.getC(); c++) {
               if (!channelList.contains(dp.getSummaryMetadata().getChannelNameList().get(c))) {
                  break;
               }
               tmpC++;
               for (int t = mins.getOrDefault(Coords.T, 0);
                    t <= maxes.getOrDefault(Coords.T, 0);
                    t++) {
                  for (int z = mins.getOrDefault(Coords.Z, 0);
                       z <= maxes.getOrDefault(Coords.Z, 0);
                       z++) {
                     if (monitor.isCanceled()) {
                        newStore.freeze();
                        if (newDataViewer == null) {
                           newStore.close();
                        }
                        SwingUtilities.invokeLater(() -> button.setEnabled(true));
                        return;
                     }
                     ImagePlus newImgPlus = IJ.createImage(
                           "Stitched image-" + newP, "16-bit black", newWidth, newHeight, 2);
                     boolean imgAdded = false;
                     for (int p = 0; p < mistEntries.size(); p++) {
                        if (monitor.isCanceled()) {
                           newStore.freeze();
                           if (newDataViewer == null) {
                              newStore.close();
                           }
                           SwingUtilities.invokeLater(() -> button.setEnabled(true));
                           return;
                        }
                        Image img = null;
                        Coords coords = imgCb.c(c).t(t).z(z)
                              .p(newP * mistEntries.size() + p)
                              .build();
                        if (dp.hasImage(coords)) {
                           img = dp.getImage(coords);
                        }
                        if (img != null) {
                           imgAdded = true;
                           String posName = img.getMetadata().getPositionName("");
                           MistGlobalData msg = null;
                           if (PositionConvention.HCS.equals(positionConvention)) {
                              int siteNr = Integer.parseInt(posName.substring(
                                    posName.lastIndexOf('_')
                                          + 1));
                              for (MistGlobalData entry : mistEntries) {
                                 if (entry.getSiteNr() == siteNr) {
                                    msg = entry;
                                    break;
                                 }
                              }
                           } else {
                              for (MistGlobalData entry : mistEntries) {
                                 if (entry.getSiteNr() == p) {
                                    msg = entry;
                                    break;
                                 }
                              }
                           }
                           if (msg == null) {
                              studio.logs().showError("Did not find specified image");
                              SwingUtilities.invokeLater(() -> button.setEnabled(true));
                              return;
                           }
                           ImageProcessor ip = DefaultImageJConverter.createProcessor(img,
                                 false);
                           newImgPlus.getProcessor().insert(ip, msg.getPositionX(),
                                 msg.getPositionY());
                        }
                     }
                     if (imgAdded) {
                        Image newImg = studio.data().ij().createImage(newImgPlus.getProcessor(),
                              imgCb.c(tmpC).t(t - mins.getOrDefault(Coords.T, 0))
                                    .z(z - mins.getOrDefault(Coords.Z, 0))
                                    .p(newP).build(),
                              dp.getImage(imgCb.c(c).t(t).z(z).p(newP * mistEntries.size())
                                    .build()).getMetadata().copyBuilderWithNewUUID().build());
                        newStore.putImage(newImg);
                        nrImages++;
                        final int count = nrImages;
                        SwingUtilities.invokeLater(() -> monitor.setProgress(count));
                        int processTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
                        updatableAlert.setText("Processed " + nrImages + " images of "
                              + maxNumImages + " in " + processTime + " seconds");
                     }
                  }
               }
            }
         }
      } catch (IOException e) {
         studio.logs().showError("Error creating new data store: " + e.getMessage());
      } catch (NullPointerException npe) {
         studio.logs().showError("Coding error in Mist plugin: " + npe.getMessage());
      } finally {
         try {
            newStore.freeze();
            if (newDataViewer == null) {
               newStore.close();
            }
         } catch (IOException ioe) {
            studio.logs().logError(ioe, "IO Error while freezing DataProvider");
         }

         SwingUtilities.invokeLater(() -> {
            SwingUtilities.invokeLater(() -> monitor.setProgress(maxNumImages));
            button.setEnabled(true);
         });
      }
   }
}

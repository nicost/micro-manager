///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------

//AUTHOR:       Nico Stuurman, nico@cmp.ucsf.edu, January 10, 2008
//              Code added by nstuurman@altoslabs.com, 2022

//COPYRIGHT:    University of California, San Francisco, 2008 - 2014
//              Altoslabs, 2022

//LICENSE:      This file is distributed under the BSD license.
//License text is included with the source distribution.

//This file is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty
//of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

//IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.internal.positionlist;

import com.google.common.eventbus.Subscribe;
import java.awt.Font;
import java.awt.Toolkit;
import java.text.ParseException;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import mmcorej.CMMCore;
import mmcorej.StrVector;
import org.micromanager.MultiStagePosition;
import org.micromanager.PositionList;
import org.micromanager.StagePosition;
import org.micromanager.Studio;
import org.micromanager.events.PixelSizeChangedEvent;
import org.micromanager.events.ShutdownCommencingEvent;
import org.micromanager.internal.positionlist.utils.TileCreator;
import org.micromanager.internal.positionlist.utils.ZGenerator;
import org.micromanager.internal.utils.NumberUtils;
import org.micromanager.internal.utils.ReportingUtils;
import org.micromanager.internal.utils.WindowPositioning;
import org.micromanager.propertymap.MutablePropertyMapView;

/**
 * Makes the Dialog that creates Position Lists with Grids or lines.
 */
public final class TileCreatorDlg extends JDialog {
   private static final long serialVersionUID = 1L;
   private final CMMCore core_;
   private final Studio studio_;
   private final TileCreator tileCreator_;
   private final MultiStagePosition[] endPosition_;
   private final boolean[] endPositionSet_;
   private final PositionListDlg positionListDlg_;
   private final JTextField overlapField_;
   private TileCreator.OverlapUnitEnum overlapUnit_ = TileCreator.OverlapUnitEnum.UM;
   private int centeredFrames_ = 0;
   private final JTextField pixelSizeField_;
   private final JLabel labelLeft_ = new JLabel();
   private final JLabel labelTop_ = new JLabel();
   private final JLabel labelRight_ = new JLabel();
   private final JLabel labelBottom_ = new JLabel();
   private final JLabel labelWidth_ = new JLabel();
   private final JLabel labelWidthUmPx_ = new JLabel();
   private static int numericPrefix_ = 0;

   private static final String OVERLAP_PREF = "overlap";
   private static final String OVERLAP_UNIT_PREF = "overlap_unit";
   private static final String PREFIX_PREF = "prefix";
   private static final String GRID_SELECTED = "grid_selected";

   /**
    * Create the dialog.
    *
    * @param core            - Micro-Manager Core object
    * @param studio          - the Micro-Manager UI
    * @param positionListDlg - The position list dialog
    */
   public TileCreatorDlg(final CMMCore core, final Studio studio,
                         final PositionListDlg positionListDlg) {
      super();
      super.setResizable(false);
      super.setName("tileDialog");
      super.getContentPane().setLayout(null);

      core_ = core;
      studio_ = studio;
      tileCreator_ = new TileCreator(core_, this.getRootPane());
      positionListDlg_ = positionListDlg;
      positionListDlg_.activateAxisTable(false);
      endPosition_ = new MultiStagePosition[4];
      endPositionSet_ = new boolean[4];

      final MutablePropertyMapView settings = studio.profile().getSettings(
            TileCreatorDlg.class);

      super.setTitle("Tile Creator");
      final Font plainFont10 = new Font("", Font.PLAIN, 10);
      final Font plainFont8 = new Font("", Font.PLAIN, 8);

      super.setIconImage(Toolkit.getDefaultToolkit().getImage(
            getClass().getResource("/org/micromanager/icons/microscope.gif")));
      super.setLocation(300, 300);
      WindowPositioning.setUpLocationMemory(this, this.getClass(), null);
      super.setSize(344, 280);

      final JButton goToLeftButton = new JButton();
      goToLeftButton.setFont(plainFont10);
      goToLeftButton.setText("Go To");
      goToLeftButton.setBounds(20, 89, 93, 23);
      super.getContentPane().add(goToLeftButton);
      goToLeftButton.addActionListener(arg0 -> {
         if (endPositionSet_[3]) {
            goToPosition(endPosition_[3]);
         }
      });

      labelLeft_.setFont(plainFont10);
      labelLeft_.setHorizontalAlignment(JLabel.CENTER);
      labelLeft_.setText("");
      labelLeft_.setBounds(0, 112, 130, 14);
      super.getContentPane().add(labelLeft_);

      final JButton setLeftButton = new JButton();
      setLeftButton.setBounds(20, 66, 93, 23);
      setLeftButton.setFont(plainFont10);
      setLeftButton.addActionListener(arg0 -> labelLeft_.setText(thisPosition(markPosition(3))));
      setLeftButton.setText("Set");
      super.getContentPane().add(setLeftButton);

      labelTop_.setFont(plainFont8);
      labelTop_.setHorizontalAlignment(JLabel.CENTER);
      labelTop_.setText("");
      labelTop_.setBounds(115, 51, 130, 14);
      super.getContentPane().add(labelTop_);

      final JButton goToTopButton = new JButton();
      goToTopButton.setFont(plainFont10);
      goToTopButton.setText("Go To");
      goToTopButton.setBounds(129, 28, 93, 23);
      super.getContentPane().add(goToTopButton);
      goToTopButton.addActionListener(arg0 -> {
         if (endPositionSet_[0]) {
            goToPosition(endPosition_[0]);
         }
      });

      final JButton setTopButton = new JButton();
      setTopButton.addActionListener(arg0 -> labelTop_.setText(thisPosition(markPosition(0))));
      setTopButton.setBounds(129, 5, 93, 23);
      setTopButton.setFont(plainFont10);
      setTopButton.setText("Set");
      super.getContentPane().add(setTopButton);

      labelRight_.setFont(plainFont8);
      labelRight_.setHorizontalAlignment(JLabel.CENTER);
      labelRight_.setText("");
      labelRight_.setBounds(214, 112, 130, 14);
      super.getContentPane().add(labelRight_);

      final JButton setRightButton = new JButton();
      setRightButton.addActionListener(arg0 -> labelRight_.setText(thisPosition(markPosition(1))));
      setRightButton.setBounds(234, 66, 93, 23);
      setRightButton.setFont(plainFont10);
      setRightButton.setText("Set");
      super.getContentPane().add(setRightButton);

      labelBottom_.setFont(plainFont8);
      labelBottom_.setHorizontalAlignment(JLabel.CENTER);
      labelBottom_.setText("");
      labelBottom_.setBounds(115, 172, 130, 14);
      super.getContentPane().add(labelBottom_);

      final JButton setBottomButton = new JButton();
      setBottomButton.addActionListener(
            arg0 -> labelBottom_.setText(thisPosition(markPosition(2))));
      setBottomButton.setFont(plainFont10);
      setBottomButton.setText("Set");
      setBottomButton.setBounds(129, 126, 93, 23);
      super.getContentPane().add(setBottomButton);

      final JButton goToRightButton = new JButton();
      goToRightButton.setFont(plainFont10);
      goToRightButton.setText("Go To");
      goToRightButton.setBounds(234, 89, 93, 23);
      super.getContentPane().add(goToRightButton);
      goToRightButton.addActionListener(arg0 -> {
         if (endPositionSet_[1]) {
            goToPosition(endPosition_[1]);
         }
      });

      final JButton goToBottomButton = new JButton();
      goToBottomButton.setFont(plainFont10);
      goToBottomButton.setText("Go To");
      goToBottomButton.setBounds(129, 149, 93, 23);
      super.getContentPane().add(goToBottomButton);
      goToBottomButton.addActionListener(arg0 -> {
         if (endPositionSet_[2]) {
            goToPosition(endPosition_[2]);
         }
      });

      final JButton gridCenteredHereButton = new JButton();
      gridCenteredHereButton.setFont(plainFont10);
      gridCenteredHereButton.setText("Center Here");
      gridCenteredHereButton.setBounds(129, 66, 93, 23);
      gridCenteredHereButton.addActionListener(arg0 -> {
         try {
            centerGridHere();
         } catch (TileCreatorException tex) {
            // zero pixel size exception. User was already told
         }
      });
      super.getContentPane().add(gridCenteredHereButton);

      final JButton centeredPlusButton = new JButton();
      centeredPlusButton.setFont(plainFont10);
      centeredPlusButton.setText("+");
      centeredPlusButton.setBounds(184, 89, 38, 19);
      centeredPlusButton.addActionListener(arg0 -> {
         ++centeredFrames_;
         labelWidth_.setText(String.format("%dx%d", centeredFrames_, centeredFrames_));
         updateCenteredSizeLabel();

      });
      super.getContentPane().add(centeredPlusButton);

      labelWidth_.setFont(plainFont8);
      labelWidth_.setHorizontalAlignment(JLabel.CENTER);
      labelWidth_.setText("");
      labelWidth_.setBounds(157, 89, 37, 19);
      super.getContentPane().add(labelWidth_);

      final JButton centeredMinusButton = new JButton();
      centeredMinusButton.setFont(plainFont10);
      centeredMinusButton.setText("-");
      centeredMinusButton.setBounds(129, 89, 38, 19);
      centeredMinusButton.addActionListener(arg0 -> {
         --centeredFrames_;
         if (centeredFrames_ < 1) {
            centeredFrames_ = 1;
         }
         labelWidth_.setText(String.format("%dx%d", centeredFrames_, centeredFrames_));
         updateCenteredSizeLabel();
      });
      super.getContentPane().add(centeredMinusButton);

      labelWidthUmPx_.setFont(plainFont8);
      labelWidthUmPx_.setHorizontalAlignment(JLabel.CENTER);
      labelWidthUmPx_.setText("");
      labelWidthUmPx_.setBounds(129, 108, 93, 14);
      super.getContentPane().add(labelWidthUmPx_);


      final JComponent[] lineDisableComponents = {
            goToBottomButton, goToTopButton, labelTop_,
            setBottomButton, setTopButton, labelBottom_};

      final JRadioButton gridButton = new JRadioButton("Grid");
      gridButton.setBounds(20, 130, 80, 14);
      gridButton.setFont(plainFont10);
      gridButton.addActionListener(e -> {
         for (JComponent component : lineDisableComponents) {
            component.setEnabled(true);
         }
         settings.putBoolean(GRID_SELECTED, true);
      });
      gridButton.setSelected(settings.getBoolean(GRID_SELECTED, true));
      super.getContentPane().add(gridButton);

      final JRadioButton lineButton = new JRadioButton("Line");
      lineButton.setBounds(20, 150, 80, 14);
      lineButton.setFont(plainFont10);
      lineButton.addActionListener(e -> {
         for (JComponent component : lineDisableComponents) {
            component.setEnabled(false);
         }
         settings.putBoolean(GRID_SELECTED, false);
      });
      lineButton.setSelected(!settings.getBoolean(GRID_SELECTED, false));
      super.getContentPane().add(lineButton);

      if (lineButton.isSelected()) {
         for (JComponent component : lineDisableComponents) {
            component.setEnabled(false);
         }
      }

      ButtonGroup buttonGroup = new ButtonGroup();
      buttonGroup.add(gridButton);
      buttonGroup.add(lineButton);

      final JLabel overlapLabel = new JLabel();
      overlapLabel.setFont(plainFont10);
      overlapLabel.setText("Overlap");
      overlapLabel.setBounds(5, 189, 40, 14);
      super.getContentPane().add(overlapLabel);

      overlapField_ = new JTextField();
      overlapField_.setBounds(45, 186, 40, 20);
      overlapField_.setFont(plainFont10);
      overlapField_.setText(settings.getString(OVERLAP_PREF, "0"));
      overlapField_.addActionListener(arg0 -> {
         settings.putString(OVERLAP_PREF, overlapField_.getText());
         updateCenteredSizeLabel();
      });
      super.getContentPane().add(overlapField_);

      String[] unitStrings = {"um", "px", "%"};
      JComboBox<String> overlapUnitsCombo = new JComboBox<>(unitStrings);
      overlapUnitsCombo.setSelectedItem(settings.getString(OVERLAP_UNIT_PREF, unitStrings[0]));
      overlapUnit_ = TileCreator.OverlapUnitEnum.values()[overlapUnitsCombo.getSelectedIndex()];
      overlapUnitsCombo.addActionListener(arg0 -> {
         JComboBox<String> cb = (JComboBox<String>) arg0.getSource();
         overlapUnit_ = TileCreator.OverlapUnitEnum.values()[cb.getSelectedIndex()];
         settings.putString(OVERLAP_UNIT_PREF, unitStrings[cb.getSelectedIndex()]);
         updateCenteredSizeLabel();
      });
      overlapUnitsCombo.setBounds(90, 186, 40, 20);
      super.getContentPane().add(overlapUnitsCombo);

      final JLabel pixelSizeLabel = new JLabel();
      pixelSizeLabel.setFont(plainFont10);
      pixelSizeLabel.setText("Pixel Size [um]");
      pixelSizeLabel.setBounds(140, 189, 80, 14);
      super.getContentPane().add(pixelSizeLabel);

      pixelSizeField_ = new JTextField();
      pixelSizeField_.setFont(plainFont10);
      pixelSizeField_.setBounds(215, 186, 35, 20);
      pixelSizeField_.setText(NumberUtils.doubleToDisplayString(core_.getPixelSizeUm()));
      pixelSizeField_.addActionListener(arg0 -> updateCenteredSizeLabel());
      super.getContentPane().add(pixelSizeField_);

      final JLabel prefixLabel = new JLabel();
      prefixLabel.setFont(plainFont10);
      prefixLabel.setText("Prefix");
      prefixLabel.setBounds(260, 189, 30, 14);
      super.getContentPane().add(prefixLabel);

      JTextField prefixField = new JTextField();
      prefixField.setBounds(290, 186, 40, 20);
      prefixField.setFont(plainFont10);
      prefixField.setText(settings.getString(PREFIX_PREF, "Pos"));
      super.getContentPane().add(prefixField);

      final JButton okButton = new JButton();
      okButton.setFont(plainFont10);
      okButton.setText("OK");
      okButton.addActionListener(arg0 -> {
         settings.putString(OVERLAP_PREF, overlapField_.getText());
         settings.putString(PREFIX_PREF, prefixField.getText());
         addToPositionList();
      });
      okButton.setBounds(20, 216, 93, 23);
      super.getContentPane().add(okButton);

      final JButton cancelButton = new JButton();
      cancelButton.setBounds(129, 216, 93, 23);
      cancelButton.setFont(plainFont10);
      cancelButton.addActionListener(arg0 -> dispose());
      cancelButton.setText("Cancel");
      super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      super.getContentPane().add(cancelButton);

      final JButton resetButton = new JButton();
      resetButton.setBounds(234, 216, 93, 23);
      resetButton.setFont(plainFont10);
      resetButton.addActionListener(arg0 -> reset());
      resetButton.setText("Reset");
      super.getContentPane().add(resetButton);

      studio.events().registerForEvents(this);
   }

   @Override
   public void dispose() {
      studio_.profile().getSettings(TileCreatorDlg.class).putString(
            OVERLAP_PREF, overlapField_.getText());
      positionListDlg_.activateAxisTable(true);
      super.dispose();
   }

   /**
    * Handles event that signals that Micro-Manager is shutting down.
    *
    * @param se the event.
    */
   @Subscribe
   public void shuttingDown(ShutdownCommencingEvent se) {
      if (se.isCanceled()) {
         return;
      }

      studio_.profile().getSettings(TileCreatorDlg.class).putString(
            OVERLAP_PREF, overlapField_.getText());
      dispose();
   }

   /**
    * Store current xyPosition.
    * Only store positions of drives selected in PositionList
    */
   private MultiStagePosition markPosition(int location) {
      MultiStagePosition msp = new MultiStagePosition();

      try {
         // read 1-axis stages
         final StrVector zStages = positionListDlg_.get1DAxes();
         if (zStages.size() > 0) {
            msp.setDefaultZStage(zStages.get(0));
            for (int i = 0; i < zStages.size(); i++) {
               StagePosition sp = StagePosition.create1D(
                     zStages.get(i), core_.getPosition(zStages.get(i)));
               msp.add(sp);
            }
         }

         // and 2 axis default stage
         final String xyStage = positionListDlg_.get2DAxis();
         if (xyStage != null) {
            msp.setDefaultXYStage(xyStage);
            StagePosition sp = StagePosition.create2D(
                  xyStage, core_.getXPosition(xyStage), core_.getYPosition(xyStage));
            msp.add(sp);
         }
      } catch (Exception e) {
         ReportingUtils.showError(e, this);
      }

      endPosition_[location] = msp;
      endPositionSet_[location] = true;

      return msp;

   }

   /**
    * Update display of the current xy position.
    */
   private String thisPosition(MultiStagePosition msp) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < msp.size(); i++) {
         StagePosition sp = msp.get(i);
         sb.append(sp.getVerbose()).append("\n");
      }

      return sb.toString();
   }

   /**
    * Updates the labelWidthUmPx_ field when the number of frames in
    * a centered grid is changed.
    */

   private void updateCenteredSizeLabel() {
      try {
         double[] centeredSize = getCenteredSize();
         if (centeredSize[0] == 0.0) {
            labelWidthUmPx_.setText("");
         } else {
            labelWidthUmPx_.setText((int) centeredSize[0] + "x" + (int) centeredSize[1] + "um");
         }
      } catch (TileCreatorException tex) {
         // most likely zero pixel size no need to update
      }
   }

   /**
    * Compute the um size of an nxn grid.
    */

   private double[] getCenteredSize() throws TileCreatorException {
      double pixelSizeUm = getPixelSizeUm();
      double imageSizeXUm = tileCreator_.getImageSize(pixelSizeUm)[0];
      double imageSizeYUm = tileCreator_.getImageSize(pixelSizeUm)[1];

      double overlap = getOverlap();
      double tileSizeXUm = tileCreator_.getTileSize(overlap, overlapUnit_, pixelSizeUm)[0];
      double tileSizeYUm = tileCreator_.getTileSize(overlap, overlapUnit_, pixelSizeUm)[1];

      double overlapXUm = imageSizeXUm - tileSizeXUm;
      double overlapYUm = imageSizeYUm - tileSizeYUm;

      double totalXUm = tileSizeXUm * centeredFrames_ + overlapXUm;
      double totalYUm = tileSizeYUm * centeredFrames_ + overlapYUm;

      return new double[] {totalXUm, totalYUm};
   }

   /**
    * Updates all four positions to create a grid that is centered at the
    * current location and has a total diameter with the specified number
    * of frames.
    */
   private void centerGridHere() throws TileCreatorException {
      double pixelSizeUm = getPixelSizeUm();
      double imageSizeXUm = tileCreator_.getImageSize(pixelSizeUm)[0];
      double imageSizeYUm = tileCreator_.getImageSize(pixelSizeUm)[1];

      double[] centeredSize = getCenteredSize();
      if (centeredSize[0] == 0.0) {
         return;
      }

      double offsetXUm = centeredSize[0] / 2.0 - imageSizeXUm / 2.0 - 1;
      double offsetYUm = centeredSize[1] / 2.0 - imageSizeYUm / 2.0 - 1;

      for (int location = 0; location < 4; ++location) {
         // get the current position
         MultiStagePosition msp = new MultiStagePosition();
         StringBuilder sb = new StringBuilder();

         // read 1-axis stages
         try {
            final StrVector zStages = positionListDlg_.get1DAxes();
            if (zStages.size() > 0) {
               msp.setDefaultZStage(zStages.get(0));
               for (int i = 0; i < zStages.size(); i++) {
                  StagePosition sp = StagePosition.create1D(
                        zStages.get(i), core_.getPosition(zStages.get(i)));
                  msp.add(sp);
                  sb.append(sp.getVerbose()).append("\n");
               }
            }

            // read 2-axis stages
            final String xyStage = positionListDlg_.get2DAxis();
            if (xyStage != null) {
               msp.setDefaultXYStage(xyStage);
               StagePosition sp = StagePosition.create2D(
                     xyStage, core_.getXPosition(xyStage), core_.getYPosition(xyStage));

               switch (location) {
                  case 0: // top
                     sp.set2DPosition(xyStage,
                           core_.getXPosition(xyStage),
                           core_.getYPosition(xyStage) + offsetYUm);
                     break;
                  case 1: // right
                     sp.set2DPosition(xyStage,
                           core_.getXPosition(xyStage) + offsetXUm,
                           core_.getYPosition(xyStage));
                     break;
                  case 2: // bottom
                     sp.set2DPosition(xyStage,
                           core_.getXPosition(xyStage),
                           core_.getYPosition(xyStage) - offsetYUm);
                     break;
                  case 3: // left
                     sp.set2DPosition(xyStage,
                           core_.getXPosition(xyStage) - offsetXUm,
                           core_.getYPosition(xyStage));
                     break;
                  default:
                     throw new IllegalStateException("Unexpected value: " + location);
               }
               msp.add(sp);
               sb.append(sp.getVerbose()).append("\n");
            }
         } catch (Exception e) {
            ReportingUtils.showError(e, this);
         }

         endPosition_[location] = msp;
         endPositionSet_[location] = true;

         switch (location) {
            case 0: // top
               labelTop_.setText(sb.toString());
               break;
            case 1: // right
               labelRight_.setText(sb.toString());
               break;
            case 2: // bottom
               labelBottom_.setText(sb.toString());
               break;
            case 3: // left
               labelLeft_.setText(sb.toString());
               break;
            default:
               throw new IllegalStateException("Unexpected value: " + location);
         }
      }
   }

   private double getPixelSizeUm() throws TileCreatorDlg.TileCreatorException {
      // check if we are calibrated, TODO: allow input of image size
      double pixSizeUm = 0.0;
      try {
         pixSizeUm = NumberUtils.displayStringToDouble(pixelSizeField_.getText());
      } catch (ParseException e) {
         ReportingUtils.logError(e);
      }
      if (pixSizeUm <= 0.0) {
         JOptionPane.showMessageDialog(this,
               "Pixel Size should be a value > 0 (usually 0.1 -1 um).  "
                     + "It should be experimentally determined. ");
         throw new TileCreatorDlg.TileCreatorException("Zero pixel size");
      }

      return pixSizeUm;
   }

   private double getOverlap() {
      try {
         return NumberUtils.displayStringToDouble(overlapField_.getText());
      } catch (ParseException e) {
         ReportingUtils.logError(e, "Number Parse error in Tile Creator Dialog");
         return 0;
      }
   }

   /**
    * Create the tile list based on user input, pixelsize, and imagesize.
    */
   private void addToPositionList() {
      // Sanity check: don't create any positions if there is no XY stage to
      // use.
      String xyStage = positionListDlg_.get2DAxis();
      if (xyStage == null) {
         return;
      }
      numericPrefix_ += 1;
      double overlap = getOverlap();
      double pixelSizeUm;
      try {
         pixelSizeUm = getPixelSizeUm();
      } catch (TileCreatorDlg.TileCreatorException ex) {
         ReportingUtils.showError(ex, this);
         return;
      }
      final MutablePropertyMapView settings = studio_.profile().getSettings(TileCreatorDlg.class);
      StrVector zStages = positionListDlg_.get1DAxes();
      PositionList posList;
      final PositionList endPoints = new PositionList();
      String prefix = settings.getString(PREFIX_PREF, "Pos");
      if (settings.getBoolean(GRID_SELECTED, true)) {
         for (MultiStagePosition multiStagePosition : endPosition_) {
            // We don't want to send null positions to the tile creator.
            if (multiStagePosition != null) {
               endPoints.addPosition(multiStagePosition);
            }
         }
         posList = tileCreator_.createTiles(overlap, overlapUnit_,
               endPoints.getPositions(), pixelSizeUm, prefix + "-" + numericPrefix_,
               xyStage, zStages, ZGenerator.Type.SHEPINTERPOLATE);
      } else {
         if (endPosition_[1] == null || endPosition_[3] == null) {
            studio_.logs().showError("Please set the left and right positions", this);
            return;
         }
         endPoints.addPosition(endPosition_[3]); // left
         endPoints.addPosition(endPosition_[1]); // right
         posList = tileCreator_.createLine(overlap, overlapUnit_,
               endPoints.getPositions(), pixelSizeUm, prefix + "-" + numericPrefix_,
               xyStage, zStages, ZGenerator.Type.SHEPINTERPOLATE);
      }
      // Add to position list
      // Increment prefix for these positions
      if (posList != null) {
         MultiStagePosition[] msps = posList.getPositions();
         for (MultiStagePosition msp : msps) {
            positionListDlg_.addPosition(msp, msp.getLabel());
         }
         positionListDlg_.activateAxisTable(true);
         dispose();
      }
   }

   /**
    * Delete all positions from the dialog and update labels. Re-read pixel
    * calibration - when available - from the core
    */
   private void reset() {
      for (int i = 0; i < 4; i++) {
         endPositionSet_[i] = false;
      }
      labelTop_.setText("");
      labelRight_.setText("");
      labelBottom_.setText("");
      labelLeft_.setText("");
      labelWidth_.setText("");
      labelWidthUmPx_.setText("");

      double pxsz = core_.getPixelSizeUm();
      pixelSizeField_.setText(NumberUtils.doubleToDisplayString(pxsz));
      centeredFrames_ = 0;
   }

   /**
    * Move stage to position.
    */
   private void goToPosition(MultiStagePosition position) {
      try {
         MultiStagePosition.goToPosition(position, core_);
      } catch (Exception e) {
         ReportingUtils.logError(e);
      }
   }

   /**
    * Handles the event signalling that the pixel size changed.
    *
    * @param event the event.
    */
   @Subscribe
   public void onPixelSizeChanged(PixelSizeChangedEvent event) {
      pixelSizeField_.setText(NumberUtils.doubleToDisplayString(
            event.getNewPixelSizeUm()));
      updateCenteredSizeLabel();
   }

   private class TileCreatorException extends Exception {

      private static final long serialVersionUID = -84723856111238971L;
      private Throwable cause;
      private static final String MSG_PREFIX = "MMScript error: ";

      public TileCreatorException(String message) {
         super(MSG_PREFIX + message);
      }

      public TileCreatorException(Throwable t) {
         super(MSG_PREFIX + t.getMessage());
         this.cause = t;
      }

      @Override
      public Throwable getCause() {
         return this.cause;
      }
   }
}

/*
 * DualAndorFrame.java
 *
 * Created on Oct 29, 2010, 11:32:29 AM
 *
 * Copyright UCSF, 2010
 *
 * Author: Nico Stuurman: nico at cmp.ucsf.edu
 *
 */

package org.micromanager.multicamera;

import com.google.common.eventbus.Subscribe;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.text.ParseException;

import javax.swing.*;

import java.awt.event.ItemEvent;
import java.util.ArrayList;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import mmcorej.StrVector;

import org.micromanager.events.PropertiesChangedEvent;
import org.micromanager.events.PropertyChangedEvent;
import org.micromanager.Studio;

// Imports for MMStudio internal packages
// Plugins should not access internal packages, to ensure modularity and
// maintainability. However, this plugin code is older than the current
// MMStudio API, so it still uses internal classes and interfaces. New code
// should not imitate this practice.
import org.micromanager.internal.utils.NumberUtils;
import org.micromanager.internal.utils.WindowPositioning;

/**
 * @author Nico Stuurman
 */
public class MultiCameraFrame extends JFrame {
   private static final long serialVersionUID = 1L;
   private final Studio studio_;
   private final CMMCore core_;
   private int EMGainMin_ = 4;
   private int EMGainMax_ = 1000;
   private ArrayList<String> camerasInUse_;
   private String coreCamera_;
   private boolean initialized_ = false;
   private volatile boolean externalCameraChange_ = false;
   private static final String MIXED = "";
   private static final int MIXEDINT = -1;
   private static final String MODE = "Output_Amplifier";
   private static final String MODECONV16 = "Conventional-16bit";
   private static final String MODEEM14 = "EM-14bit";
   private static final String MODEEM16 = "EM-16bit";
   private static final String MODECONV = "Conventional";
   private static final String MODEEM = "EM";
   private static final String EMMODE = "Electron Multiplying";
   private static final String NORMALMODE = "Conventional";
   private static final String ADCONVERTER = "AD_Converter";
   private static final String AD14BIT = "1. 14bit";
   private static final String AD16BIT = "2. 16bit";
   private static final String EMGAIN = "Gain";
   private static final String EMSWITCH = "EMSwitch";
   private static final String AMPGAIN = "Pre-Amp-Gain";
   private static final String FRAMETRANSFER = "FrameTransfer";
   // Andor calls this readout mode
   private static final String SPEED = "ReadoutMode";
   private static final String TRIGGER = "Trigger";
   private static final String TEMP = "CCDTemperature";
   private static final String PHYSCAM1 = "Physical Camera 1";
   private static final String PHYSCAM2 = "Physical Camera 2";
   private static final String PHYSCAM3 = "Physical Camera 3";
   private static final String PHYSCAM4 = "Physical Camera 4";

   /**
    * Creates new form MultiCameraFrame.
    *
    * @param studio - handle to instance of the Studio
    * @throws java.lang.Exception Not sure what Exceptions this can generate.
    */
   public MultiCameraFrame(Studio studio) throws Exception {
      studio_ = studio;
      core_ = studio_.getCMMCore();

      mmcorej.StrVector cameras = core_.getLoadedDevicesOfType(DeviceType.CameraDevice);
      String[] cameras_ = cameras.toArray();

      if (cameras_.length < 1) {
         studio_.logs().showError("This plugin needs at least one camera");
         throw new IllegalArgumentException("This plugin needs at least one camera");
      }

      String currentCamera = core_.getCameraDevice();
      updateCamerasInUse(currentCamera);

      core_.getImageWidth();
      core_.getImageHeight();

      // find the first non-multi-channel camera to test properties
      String testCamera = camerasInUse_.get(0);

      initComponents();

      super.setIconImage(Toolkit.getDefaultToolkit().getImage(
            getClass().getResource("/org/micromanager/icons/microscope.gif")));
      super.setLocation(100, 100);
      WindowPositioning.setUpLocationMemory(this, this.getClass(), null);

      cameraSelectComboBox.removeAllItems();

      for (String camera : cameras_) {
         cameraSelectComboBox.addItem(camera);
      }

      cameraSelectComboBox.setSelectedItem(currentCamera);
      if (cameras_.length <= 1) {
         cameraSelectComboBox.setEnabled(false);
      }

      adjustUIToCamera(testCamera);

      updateTemp();

      initialized(true, true);

      core_.setCameraDevice(currentCamera);
   }

   private void adjustUIToCamera(String testCamera) throws Exception {
      if (!core_.hasProperty(testCamera, MODE)) {
         jLabel5.setEnabled(false);
         modeComboBox.setEnabled(false);
      }
      else {
         modeComboBox.removeAllItems();
         modeComboBox.addItem(MIXED);
         if (core_.hasProperty(testCamera, ADCONVERTER)) {
            modeComboBox.addItem(MODECONV16);
            modeComboBox.addItem(MODEEM14);
            modeComboBox.addItem(MODEEM16);
         }
         else {
            modeComboBox.addItem(MODECONV);
            modeComboBox.addItem(MODEEM);
         }
         modeComboBox.setSelectedItem(getMode());
      }

      if (!core_.hasProperty(testCamera, EMGAIN)) {
         jLabel4.setEnabled(false);
         EMGainTextField.setEnabled(false);
         EMGainSlider.setEnabled(false);
      }
      else {
         EMGainMin_ = (int) core_.getPropertyLowerLimit(testCamera, EMGAIN);
         EMGainMax_ = (int) core_.getPropertyUpperLimit(testCamera, EMGAIN);
         EMGainSlider.setMinimum(EMGainMin_);
         EMGainSlider.setMaximum(EMGainMax_);
         int gain = NumberUtils.coreStringToInt(core_.getProperty(testCamera, EMGAIN));
         EMGainSlider.setValue(gain);
         EMGainTextField.setText(NumberUtils.intToDisplayString(gain));

         if (!core_.hasProperty(testCamera, EMSWITCH)) {
            EMCheckBox.setEnabled(false);
         }
         else {
            String val = core_.getProperty(testCamera, EMSWITCH);
            if (val.equals("On")) {
               EMCheckBox.setSelected(true);
            }
         }
      }

      // Pre-amp Gain
      if (!core_.hasProperty(testCamera, AMPGAIN)) {
         GainLabel.setEnabled(false);
         gainComboBox.setEnabled(false);
      }
      else {
         updateItems(gainComboBox, AMPGAIN);
      }

      // Readout speed
      if (!core_.hasProperty(testCamera, SPEED)) {
         SpeedLabel.setEnabled(false);
         speedComboBox.setEnabled(false);
      }
      else {
         updateItems(speedComboBox, SPEED);
      }

      // Frame Transfer
      if (!core_.hasProperty(testCamera, FRAMETRANSFER)) {
         FrameTransferLabel.setEnabled(false);
         frameTransferComboBox.setEnabled(false);
      }
      else {
         updateItems(frameTransferComboBox, FRAMETRANSFER);
      }

      // Trigger
      if (!core_.hasProperty(testCamera, TRIGGER)) {
         TriggerLabel.setEnabled(false);
         triggerComboBox.setEnabled(false);
      }
      else {
         updateItems(triggerComboBox, TRIGGER);
      }
   }

   private synchronized boolean initialized(boolean set, boolean value) {
      if (set) {
         initialized_ = value;
      }

      return initialized_;
   }

   /**
    * This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      JCheckBox jCheckBox1 = new JCheckBox();
      EMGainSlider = new JSlider();
      EMGainTextField = new JTextField();
      modeComboBox = new JComboBox<>();
      jLabel4 = new JLabel();
      jLabel5 = new JLabel();
      GainLabel = new JLabel();
      speedComboBox = new JComboBox<>();
      SpeedLabel = new JLabel();
      EMCheckBox = new JCheckBox();
      gainComboBox = new JComboBox<>();
      FrameTransferLabel = new JLabel();
      frameTransferComboBox = new JComboBox<>();
      JLabel jLabel9 = new JLabel();
      cameraSelectComboBox = new JComboBox<>();
      TriggerLabel = new JLabel();
      triggerComboBox = new JComboBox<>();
      JButton tempButton = new JButton();
      tempLabel = new JLabel();
      JLabel jLabel1 = new JLabel();

      jCheckBox1.setText("jCheckBox1");

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setTitle("Andor Control");
      setResizable(false);

      EMGainSlider.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseReleased(java.awt.event.MouseEvent evt) {
            EMGainSliderMouseReleased(evt);
         }
      });

      EMGainTextField.setFont(new java.awt.Font("Lucida Grande", Font.PLAIN, 10)); // NOI18N
      EMGainTextField.setText("4");
      EMGainTextField.addFocusListener(new java.awt.event.FocusAdapter() {
         public void focusLost(java.awt.event.FocusEvent evt) {
            EMGainTextFieldFocusLost(evt);
         }
      });
      EMGainTextField.addActionListener(this::EMGainTextFieldActionPerformed);

      modeComboBox.setFont(new java.awt.Font("Lucida Grande", Font.PLAIN, 10)); // NOI18N
      modeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"EM", "Conventional"}));
      modeComboBox.addItemListener(this::modeComboBoxItemStateChanged);

      final Font plainFont10 = new Font("Lucida Frande", Font.PLAIN, 20);

      jLabel4.setFont(plainFont10); // NOI18N
      jLabel4.setText("EM Gain");

      jLabel5.setFont(plainFont10); // NOI18N
      jLabel5.setText("Mode");

      GainLabel.setFont(plainFont10); // NOI18N
      GainLabel.setText("Gain");

      speedComboBox.setFont(plainFont10); // NOI18N
      speedComboBox
            .setModel(new DefaultComboBoxModel<>(new String[] {"1MHz", "3MHz", "5MHz", "10MHz"}));
      speedComboBox.addItemListener(this::speedComboBoxItemStateChanged);

      SpeedLabel.setFont(plainFont10); // NOI18N
      SpeedLabel.setText("Speed");

      EMCheckBox.setFont(plainFont10); // NOI18N
      EMCheckBox.setText("Use");
      EMCheckBox.addActionListener(this::EMCheckBoxActionPerformed);

      gainComboBox.setFont(plainFont10); // NOI18N
      gainComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"1", "2", "3", "4", "5"}));
      gainComboBox.addItemListener(this::gainComboBoxItemStateChanged);

      FrameTransferLabel.setFont(plainFont10); // NOI18N
      FrameTransferLabel.setText("OverlapMode");

      frameTransferComboBox.setFont(plainFont10); // NOI18N
      frameTransferComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"On", "Off"}));
      frameTransferComboBox.addItemListener(this::frameTransferComboBoxItemStateChanged);

      jLabel9.setFont(plainFont10); // NOI18N
      jLabel9.setText("Active Camera");

      cameraSelectComboBox.setFont(plainFont10); // NOI18N
      cameraSelectComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"1", "2", "4", "8"}));
      cameraSelectComboBox.addItemListener(this::cameraSelectComboBoxItemStateChanged);

      TriggerLabel.setFont(plainFont10); // NOI18N
      TriggerLabel.setText("Trigger");

      triggerComboBox.setFont(plainFont10); // NOI18N
      triggerComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"On", "Off"}));
      triggerComboBox.addItemListener(this::triggerComboBoxItemStateChanged);

      tempButton.setFont(plainFont10); // NOI18N
      tempButton.setText("Temp");
      tempButton.addActionListener(this::tempButtonActionPerformed);

      tempLabel.setText("jLabel11");

      jLabel1.setFont(plainFont10); // NOI18N
      jLabel1.setText("v 1.1");

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addGroup(layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(
                              layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                          .addGroup(layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(tempButton,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE, 63,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel5)
                                                .addComponent(jLabel4)
                                                .addComponent(GainLabel)
                                                .addComponent(SpeedLabel)
                                                .addComponent(FrameTransferLabel)
                                                .addComponent(TriggerLabel))
                                          .addGap(48, 48, 48)
                                          .addGroup(layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(speedComboBox,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE, 105,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGroup(layout.createSequentialGroup()
                                                      .addGap(50, 50, 50)
                                                      .addComponent(EMGainSlider,
                                                            javax.swing.GroupLayout.PREFERRED_SIZE,
                                                            180,
                                                            javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addComponent(triggerComboBox,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE, 105,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(modeComboBox,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE, 105,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(gainComboBox,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE, 105,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(EMCheckBox)
                                                .addComponent(EMGainTextField,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE, 49,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(frameTransferComboBox,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE, 105,
                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGroup(layout.createSequentialGroup()
                                                      .addComponent(tempLabel,
                                                            javax.swing.GroupLayout.PREFERRED_SIZE,
                                                            202,
                                                            javax.swing.GroupLayout.PREFERRED_SIZE)
                                                      .addPreferredGap(
                                                            javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                            javax.swing.GroupLayout.DEFAULT_SIZE,
                                                            Short.MAX_VALUE)
                                                      .addComponent(jLabel1))))
                                    .addGroup(layout.createSequentialGroup()
                                          .addComponent(jLabel9)
                                          .addGap(7, 7, 7)
                                          .addComponent(cameraSelectComboBox,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 191,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())
      );
      layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(
                              layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel9)
                                    .addComponent(cameraSelectComboBox,
                                          javax.swing.GroupLayout.PREFERRED_SIZE, 23,
                                          javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(
                              layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                          .addGap(6, 6, 6)
                                          .addComponent(jLabel5)
                                          .addGap(22, 22, 22)
                                          .addComponent(jLabel4)
                                          .addGap(16, 16, 16)
                                          .addComponent(GainLabel)
                                          .addGap(12, 12, 12)
                                          .addComponent(SpeedLabel)
                                          .addGap(12, 12, 12)
                                          .addComponent(FrameTransferLabel)
                                          .addGap(12, 12, 12)
                                          .addComponent(TriggerLabel))
                                    .addGroup(layout.createSequentialGroup()
                                          .addGap(90, 90, 90)
                                          .addComponent(speedComboBox,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                          .addGap(25, 25, 25)
                                          .addComponent(EMGainSlider,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                          .addGap(140, 140, 140)
                                          .addComponent(triggerComboBox,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(modeComboBox,
                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                          .addGap(65, 65, 65)
                                          .addComponent(gainComboBox,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                          .addGap(45, 45, 45)
                                          .addComponent(EMCheckBox))
                                    .addGroup(layout.createSequentialGroup()
                                          .addGap(25, 25, 25)
                                          .addComponent(EMGainTextField,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                          .addGap(115, 115, 115)
                                          .addComponent(frameTransferComboBox,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                              layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(tempButton,
                                          javax.swing.GroupLayout.PREFERRED_SIZE, 17,
                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(tempLabel)
                                    .addComponent(jLabel1))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   /*
    * If all selected cameras have the same EM Gain value, return EM Gain value
    * otherwise return -1
    */
   private int getEMGain() {
      int gain = MIXEDINT;
      try {
         for (String camera : camerasInUse_) {
            int tmpGain = NumberUtils.coreStringToInt(core_.getProperty(camera, EMGAIN));
            if (gain == -1) {
               gain = tmpGain;
            }
            if (tmpGain != gain) {
               return MIXEDINT;
            }
         }
      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      }
      return gain;
   }

   private void setEMGain() {
      if (!initialized(false, false)) {
         return;
      }
      studio_.live().setSuspended(true);
      int val = EMGainSlider.getValue();
      try {
         for (String camera : camerasInUse_) {
            setPropertyIfPossible(camera, EMGAIN, NumberUtils.intToCoreString(val));
         }
         // gui_.app().refreshGUI();
      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      } finally {
         studio_.live().setSuspended(false);
      }
      EMGainTextField.setText(NumberUtils.intToDisplayString(val));
   }

   /*
    * Signals whether the first selected camera has its EM switch on or off
    */
   private boolean getEMSwitch() {
      try {
         for (String camera : camerasInUse_) {
            if (core_.hasProperty(camera, EMSWITCH)) {
               String OnOff = core_.getProperty(camera, EMSWITCH);
               if (OnOff.equals("On")) {
                  return true;
               }
            }
            else {
               return false;
            }

         }
      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      }
      return false;
   }

   private void handleEMGainTextFieldEvent() {
      if (!initialized(false, false)) {
         return;
      }
      try {

         int val = NumberUtils.displayStringToInt(EMGainTextField.getText());
         if (val > EMGainMax_) {
            val = EMGainMax_;
         }
         if (val < EMGainMin_) {
            val = EMGainMin_;
         }
         EMGainSlider.setEnabled(true);
         EMGainSlider.setValue(val);
         setEMGain();

      } catch (ParseException e) {
         // ignore if the user types garbage
      }
   }

   private void EMGainTextFieldActionPerformed(
         java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EMGainTextFieldActionPerformed
      handleEMGainTextFieldEvent();
   }//GEN-LAST:event_EMGainTextFieldActionPerformed

   private void EMCheckBoxActionPerformed(
         java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EMCheckBoxActionPerformed
      // EM enable
      if (!initialized(false, false)) {
         return;
      }
      studio_.live().setSuspended(true);
      boolean on = EMCheckBox.isSelected();
      String command = "Off";
      if (on) {
         command = "On";
      }
      try {
         for (String camera : camerasInUse_) {
            setPropertyIfPossible(camera, EMSWITCH, command);
         }
      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      } finally {
         studio_.live().setSuspended(false);
      }
   }//GEN-LAST:event_EMCheckBoxActionPerformed

   private String getMode() {
      String mode = "";
      try {
         String fs = camerasInUse_.get(0);
         String modeProp = core_.getProperty(fs, MODE);
         if (core_.hasProperty(fs, ADCONVERTER)) {
            String adProp = core_.getProperty(fs, ADCONVERTER);
            if (modeProp.equals(EMMODE)) {
               if (adProp.equals(AD14BIT)) {
                  mode = MODEEM14;
               }
               else if (adProp.equals(AD16BIT)) {
                  mode = MODEEM16;
               }
            }
            else if (modeProp.equals(NORMALMODE)) {
               if (adProp.equals(AD16BIT)) {
                  mode = MODECONV16;
               }
            }
            for (String camera : camerasInUse_) {
               String mP = core_.getProperty(camera, MODE);
               String aP = core_.getProperty(camera, ADCONVERTER);
               if (!mP.equals(modeProp)) {
                  mode = MIXED;
               }
               if (!aP.equals(adProp)) {
                  mode = MIXED;
               }

            }
         }
         else {  // No AD Converter
            if (modeProp.equals(EMMODE)) {
               mode = MODEEM;
            }
            else {
               mode = MODECONV;
            }
            for (String camera : camerasInUse_) {
               String mP = core_.getProperty(camera, MODE);
               if (!mP.equals(modeProp)) {
                  mode = MIXED;
               }

            }
         }
      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      }
      return mode;
   }

   private void EMGainSliderMouseReleased(MouseEvent evt) {
      setEMGain();
      studio_.app().refreshGUI();
   }

   private void tempButtonActionPerformed(ActionEvent evt) {
      studio_.live().setSuspended(true);
      updateTemp();
      studio_.live().setSuspended(false);
   }

   private void modeComboBoxItemStateChanged(ItemEvent evt) {
      // Combo box selecting readout mode (EM/standard)
      if (!(evt.getStateChange() == ItemEvent.SELECTED)) {
         return;
      }
      if (!initialized(false, false)) {
         return;
      }

      Object item = modeComboBox.getSelectedItem();
      if (item == null) {
         return;
      }
      if (item.equals(MIXED)) {
         return;
      }

      studio_.live().setSuspended(true);
      String mode = item.toString();
      try {
         for (String camera : camerasInUse_) {
            switch (mode) {
               case MODEEM14:
                  core_.setProperty(camera, ADCONVERTER, AD14BIT);
                  core_.setProperty(camera, MODE, EMMODE);
                  break;
               case MODEEM16:
                  core_.setProperty(camera, ADCONVERTER, AD16BIT);
                  core_.setProperty(camera, MODE, EMMODE);
                  break;
               case MODECONV16:
                  core_.setProperty(camera, ADCONVERTER, AD16BIT);
                  core_.setProperty(camera, MODE, NORMALMODE);
                  break;
            }

         }

         updateItems(gainComboBox, AMPGAIN);
         updateItems(speedComboBox, SPEED);

         studio_.app().refreshGUI();
      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      } finally {
         studio_.live().setSuspended(false);
      }
   }

   private void gainComboBoxItemStateChanged(
         java.awt.event.ItemEvent evt) {//GEN-FIRST:event_gainComboBoxItemStateChanged
      if (!(evt.getStateChange() == ItemEvent.SELECTED)) {
         return;
      }
      setComboSelection(gainComboBox, AMPGAIN);
      studio_.app().refreshGUI();
   }

   private void speedComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {
      if (!(evt.getStateChange() == ItemEvent.SELECTED)) {
         return;
      }
      setComboSelection(speedComboBox, SPEED);
      studio_.app().refreshGUI();
   }

   private void frameTransferComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {
      if (!(evt.getStateChange() == ItemEvent.SELECTED)) {
         return;
      }
      setComboSelection(frameTransferComboBox, FRAMETRANSFER);
      studio_.app().refreshGUI();
   }

   private void triggerComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {
      if (!(evt.getStateChange() == ItemEvent.SELECTED)) {
         return;
      }
      setComboSelection(triggerComboBox, TRIGGER);
      studio_.app().refreshGUI();
   }

   private void updateCamerasInUse(String camera) {
      camerasInUse_ = new ArrayList<>();
      if (core_.getNumberOfCameraChannels() == 1) {
         camerasInUse_.add(camera);
      }
      else if (core_.getNumberOfCameraChannels() > 1) {
         try {
            if (core_.hasProperty(camera, PHYSCAM1)) {
               for (String prop : new String[] {PHYSCAM1, PHYSCAM2, PHYSCAM3, PHYSCAM4}) {
                  if (core_.hasProperty(camera, prop)) {
                     String cam = core_.getProperty(camera, prop);
                     if (!cam.equals("Undefined")) {
                        camerasInUse_.add(cam);
                     }
                  }
               }
            }
            else {
               camerasInUse_.add(coreCamera_);
            }
         } catch (Exception ex) {
            studio_.logs().logError(ex);
         }
      }
   }

   private void cameraSelectComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {
      if (!(evt.getStateChange() == ItemEvent.SELECTED)) {
         return;
      }
      if (!initialized(false, false)) {
         return;
      }

      // since the core call to set the camera will result in a call
      // back into our code that will set the externalCameraChange_ flag
      // we need to record its value now
      boolean externalCameraChange = externalCameraChange_;
      externalCameraChange_ = false;


      studio_.live().setSuspended(true);
      try {
         // Use the initialize flag to prevent pushing settings back to the hardware
         initialized(true, false);

         coreCamera_ = (String) cameraSelectComboBox.getSelectedItem();
         String currentCamera = core_.getProperty("Core", "Camera");

         if (!currentCamera.equals(coreCamera_)) {
            core_.setProperty("Core", "Camera", coreCamera_);
         }
         updateCamerasInUse(coreCamera_);

         String fsCamera = camerasInUse_.get(0);

         adjustUIToCamera(fsCamera);
         updateTemp();
         initialized(true, true);

      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      } finally {
         studio_.live().setSuspended(false);
      }
      if (!externalCameraChange) {
         studio_.app().refreshGUI();
      }
   }

   private void EMGainTextFieldFocusLost(FocusEvent evt) {
      // check if event has the correct source?
      handleEMGainTextFieldEvent();
   }

   private void updateTemp() {
      StringBuilder tempText = new StringBuilder();
      try {
         for (String camera : camerasInUse_) {
            if (core_.hasProperty(camera, TEMP)) {
               tempText.append(core_.getProperty(camera, TEMP)).append("\u00b0").append("C   ");
            }

         }
      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      }
      tempLabel.setText(tempText.toString());
   }

   private void updateItems(JComboBox<String> comboBox, String property) {
      if (comboBox != null) {
         try {
            String camera = camerasInUse_.get(0);
            StrVector vals = core_.getAllowedPropertyValues(camera, property);

            String[] newVals = new String[(int) vals.size() + 1];
            newVals[0] = MIXED;
            for (int i = 0; i < vals.size(); i++) {
               newVals[i + 1] = vals.get(i);
            }
            comboBox.setModel(new DefaultComboBoxModel<>(newVals));

         } catch (Exception ex) {
            studio_.logs()
                  .showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
         }
         getComboSelection(comboBox, property);
      }
   }

   private void getComboSelection(JComboBox<String> comboBox, String property) {
      if (comboBox == null || !comboBox.isEnabled()) {
         return;
      }
      try {
         String fs = camerasInUse_.get(0);
         String val = core_.getProperty(fs, property);
         for (String camera : camerasInUse_) {
            String tVal = core_.getProperty(camera, property);
            if (!tVal.equals(val)) {
               comboBox.setSelectedItem(MIXED);
               return;
            }

         }
         comboBox.setSelectedItem(val);
      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      }
   }

   private void setComboSelection(JComboBox<String> comboBox, String property) {
      if (!initialized(false, false)) {
         return;
      }
      studio_.live().setSuspended(true);
      String val = (String) comboBox.getSelectedItem();
      if (val == null) {
         return;
      }
      if (val.equals(MIXED)) {
         getComboSelection(comboBox, property);
         return;
      }
      try {
         for (String camera : camerasInUse_) {
            core_.setProperty(camera, property, val);
         }
      } catch (Exception ex) {
         studio_.logs().showError(ex, MultiCameraFrame.class.getName() + " encountered an error.");
      } finally {
         studio_.live().setSuspended(false);
      }
   }

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JCheckBox EMCheckBox;
   private javax.swing.JSlider EMGainSlider;
   private javax.swing.JTextField EMGainTextField;
   private javax.swing.JLabel FrameTransferLabel;
   private javax.swing.JLabel GainLabel;
   private javax.swing.JLabel SpeedLabel;
   private javax.swing.JLabel TriggerLabel;
   private javax.swing.JComboBox<String> cameraSelectComboBox;
   private javax.swing.JComboBox<String> frameTransferComboBox;
   private javax.swing.JComboBox<String> gainComboBox;
   private javax.swing.JLabel jLabel4;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JComboBox<String> modeComboBox;
   private javax.swing.JComboBox<String> speedComboBox;
   private javax.swing.JLabel tempLabel;
   private javax.swing.JComboBox<String> triggerComboBox;
   // End of variables declaration//GEN-END:variables


   @Subscribe
   public void onPropertiesChanged(PropertiesChangedEvent event) {
      updateItems(speedComboBox, SPEED);
   }

   @Subscribe
   public void onPropertyChanged(PropertyChangedEvent event) {
      final String device = event.getDevice();
      final String property = event.getProperty();
      final String value = event.getValue();
      try {
         if (device.equals("Core") && property.equals("Camera") &&
               !value.equals(cameraSelectComboBox.getSelectedItem())) {
            SwingUtilities.invokeLater(() -> {
               externalCameraChange_ = true;
               cameraSelectComboBox.setSelectedItem(value);
            });
         }
      } catch (Exception ex) {
         studio_.logs().logError(ex);
      }
   }

   private void setPropertyIfPossible(String device, String property, String value) {
      try {
         if (core_.hasProperty(device, property)) {
            core_.setProperty(device, property, value);
         }
      } catch (Exception ex) {
         studio_.logs().logError(ex);
      }
   }


}

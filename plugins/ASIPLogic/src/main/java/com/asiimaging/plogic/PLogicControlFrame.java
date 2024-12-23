/*
 * Project: ASI PLogic Control
 * License: BSD 3-clause, see LICENSE.md
 * Author: Brandon Simpson (brandon@asiimaging.com)
 * Copyright (c) 2024, Applied Scientific Instrumentation
 */

package com.asiimaging.plogic;

import com.asiimaging.plogic.ui.asigui.Button;
import com.asiimaging.plogic.ui.asigui.ProgressBar;
import com.asiimaging.plogic.ui.data.Icons;
import com.asiimaging.plogic.ui.tabs.TabPanel;
import com.asiimaging.plogic.ui.utils.BrowserUtils;
import com.asiimaging.plogic.ui.utils.DialogUtils;
import com.asiimaging.plogic.ui.utils.WindowUtils;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.JLabel;
import net.miginfocom.swing.MigLayout;
import org.micromanager.Studio;
import org.micromanager.internal.utils.WindowPositioning;

/**
 * The user interface for the plugin.
 */
public class PLogicControlFrame extends JFrame {

   private final Studio studio_;

   private TabPanel tabPanel_;
   private JLabel currentDevice_;
   private final ProgressBar progressBar_;

   private final PLogicControlModel model_;

   public PLogicControlFrame(final PLogicControlModel model, final boolean isDeviceFound) {
      model_ = Objects.requireNonNull(model);
      studio_ = model_.studio();
      progressBar_ = new ProgressBar();

      // save window position
      WindowPositioning.setUpBoundsMemory(
            this, this.getClass(), this.getClass().getSimpleName());

      if (isDeviceFound) {
         createUserInterface();
         tabPanel_.updateTabsFromController();
      } else {
         createErrorInterface();
      }
   }

   /**
    * Create the error interface.
    */
   private void createErrorInterface() {
      setTitle(PLogicControlPlugin.menuName);
      setResizable(false);

      // use MigLayout as the layout manager
      setLayout(new MigLayout(
            "insets 10 10 10 10",
            "[]20[]",
            "[]10[]"
      ));

      final JLabel lblTitle = new JLabel(PLogicControlPlugin.menuName + ": Error");
      lblTitle.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));

      final JLabel lblError = new JLabel(
            "This plugin requires a Tiger controller with a PLogic card.");
      lblError.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

      final JLabel lblHelp = new JLabel("<html>Add the <b>PLogic</b> device from <"
            + "b>TigerCommHub</b> in the <b>ASITiger</b><br> device adapter using the "
            + "<u>Hardware Configuration Wizard</u>.</html>");
      lblHelp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

      final Button btnManual = new Button("Manual", 120, 30);
      btnManual.registerListener(e -> {
         final boolean result = DialogUtils.showConfirmDialog(btnManual,
               "Open Browser", "Open the default browser and navigate to the "
                     + "Tiger Programmable Logic Card Manual?");
         if (result) {
            BrowserUtils.openWebsite(studio_, "https://asiimaging.com/docs/tiger_programmable_logic_card");
         }
      });

      // add ui elements to the panel
      add(lblTitle, "align center, wrap");
      add(lblError, "align center, wrap");
      add(lblHelp, "align center, wrap");
      add(btnManual, "align center");

      pack(); // fit window size to layout
      setIconImage(Icons.MICROSCOPE.getImage());

      // clean up resources when the frame is closed
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
   }

   /**
    * Create the user interface.
    */
   public void createUserInterface() {
      setTitle(PLogicControlPlugin.menuName);
      setResizable(false);

      // use MigLayout as the layout manager
      setLayout(new MigLayout(
            "insets 10 10 10 0",
            "[]10[]",
            "[]10[]"
      ));

      final JLabel lblTitle = new JLabel(PLogicControlPlugin.menuName);
      lblTitle.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));

      // display currently selected device
      currentDevice_ = new JLabel(model_.plc().deviceName());
      currentDevice_.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));

      // main control area
      tabPanel_ = new TabPanel(model_, this);
      progressBar_.setAbsoluteSize(400, 10);
      progressBar_.setRange(0, model_.plc().numCells() + 16);

      // add ui elements to the panel
      add(lblTitle, "split 2");
      add(currentDevice_, "gapleft 50, wrap");
      add(tabPanel_, "wrap");
      add(progressBar_, "gapleft 6");

      pack(); // fit window size to layout
      setIconImage(Icons.MICROSCOPE.getImage());

      // clean up resources when the frame is closed
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

      // register for micro-manager events
      studio_.events().registerForEvents(this);

      WindowUtils.registerWindowClosingEvent(this, event -> {
         model_.isUpdating(false);
         studio_.logs().logMessage("window closed");
      });
   }

   public void setDeviceLabel(final String text) {
      currentDevice_.setText(text);
   }

   public ProgressBar getProgressBar() {
      return progressBar_;
   }

}

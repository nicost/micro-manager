
package org.micromanager.snoutyviewer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;
import org.micromanager.data.Coords;
import org.micromanager.display.DataViewer;
import org.micromanager.display.inspector.AbstractInspectorPanelController;
import org.micromanager.snoutyviewer.uielements.ScrollerPanel;

/**
 * LICENSE: This file is distributed under the BSD license. License text is
 * included with the source distribution.
 *
 * <p>This file is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.
 *
 * <p>IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
 *
 * @author nico
 */
public final class SnoutyInspectorPanelController extends AbstractInspectorPanelController {

   private final JPanel panel_ = new JPanel();

   private SnoutyViewer viewer_;
   
   private ScrollerPanel sp_;
   private boolean animating_ = false;
   
   private static boolean expanded_ = true;

   public SnoutyInspectorPanelController() {
      super();
      
      panel_.setLayout(new MigLayout("flowx"));
      /*
      final JCheckBox attachToNewCheckBox = new JCheckBox ("Use for all");
      attachToNewCheckBox.setToolTipText("Open all new data in ClearVolume");
      attachToNew_.set(studio.profile().getBoolean(this.getClass(), 
              USE_FOR_ALL, false));
      attachToNewCheckBox.setSelected(attachToNew_.get());
      attachToNewCheckBox.addActionListener((ActionEvent e) -> {
         attachToNew_.set(attachToNewCheckBox.isSelected());
         studio.profile().setBoolean(this.getClass(), USE_FOR_ALL, 
                 attachToNewCheckBox.isSelected());
      });
      panel_.add(attachToNewCheckBox, "span 4, wrap");
      */
      
      panel_.add(new JSeparator(SwingConstants.HORIZONTAL), "span 4, growx, pushx, wrap");
           



      if (viewer_ != null) {
         sp_ = new ScrollerPanel(viewer_);
         panel_.add(sp_, "span x 4, growx, wrap");
      }

   }

   public SnoutyViewer getViewer() {
      return viewer_;
   }
     

   /*
    @Subscribe
    public void onAcquisitionStartedEvent(AcquisitionStartedEvent ase) {
       if (attachToNew_.get()) {
          try {
             CVViewer viewer = new CVViewer(studio_, ase.getDatastore());
             viewer.register();
          } catch (Exception ex) {
             studio_.logs().logError(ex);
          }
       }
    }
   */

   @Override
   public String getTitle() {
      return "Snouty Viewer Panel";
   }

   /**
    * Called whenever the panel is attached to a DataViewer.
    *
    * @param viewer - Viewer that the panel is attached to.
    */
   @Override
   public void attachDataViewer(DataViewer viewer) {
      // although this should always be a valid viewer, check anyways
      if (!(viewer instanceof DataViewer)) {
         return;
      }

      detachDataViewer();
      
      viewer_ = (SnoutyViewer) viewer;
      
      Coords intendedDimensions = viewer_.getDataProvider()
            .getSummaryMetadata().getIntendedDimensions();
      if (intendedDimensions != null) {
         if (sp_ != null) {
            sp_.stopUpdateThread();
            panel_.remove(sp_);
         }
         sp_ = new ScrollerPanel(viewer_);
         panel_.add(sp_, "span x 4, growx, wrap");
      } 
      panel_.revalidate();
      panel_.repaint();
      viewer_.registerForEvents(this);
   }

   /**
    * Very strange, but Micro-Manager never calls detachDataViewer.  We have to
    * do that ourselves in the attachDataViewer code.  This smells like a bug
    */
   @Override
   public void detachDataViewer() {
      if (viewer_ != null) {
         viewer_.unregisterForEvents(this);
      }
      if (sp_ != null) {
         sp_.stopUpdateThread();
         panel_.remove(sp_);
      }
   }

   @Override
   public boolean isVerticallyResizableByUser() {
      return false;
   }

   @Override
   public JPanel getPanel() {
      return panel_;
   }
   
   @Override
   public void setExpanded(boolean state) {
      expanded_ = state;
   }
   
   @Override
   public boolean initiallyExpand() {
      return expanded_;
   }

}

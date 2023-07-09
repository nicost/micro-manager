package org.micromanager.snoutyviewer;

import org.micromanager.Studio;
import org.micromanager.display.DataViewer;
import org.micromanager.display.inspector.InspectorPanelController;
import org.micromanager.display.inspector.InspectorPanelPlugin;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

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

@Plugin(type = InspectorPanelPlugin.class,
      priority = Priority.NORMAL,
      name = "3D (ClearVolume)",
      description = "Interact with 3D (ClearVolume) Viewer")
public final class SnoutyInspectorPanelPlugin implements InspectorPanelPlugin {

   @Override
   public boolean isApplicableToDataViewer(DataViewer viewer) {
      return viewer instanceof DataViewer;
   }

   @Override
   public InspectorPanelController createPanelController(Studio studio) {
      return new SnoutyInspectorPanelController();
   }

}

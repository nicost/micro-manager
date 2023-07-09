package org.micromanager.snoutyviewer;

import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.micromanager.display.DisplayGearMenuPlugin;
import org.micromanager.display.DisplayWindow;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

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

 * More or less boiler plate code to become a Micro-Manager 2.0 plugin
 * Most of the action happens in the SnoutyViewer class
 *
 * @author nico
 */

@Plugin(type = DisplayGearMenuPlugin.class)
public class SnoutyPlugin implements MenuPlugin, DisplayGearMenuPlugin, SciJavaPlugin {
   private Studio studio_;
   public static final String VERSION_INFO = "0.0.1";
   private static final String COPYRIGHT_NOTICE = "";
   private static final String DESCRIPTION = "View deskewed data on the fly";
   private static final String NAME = "LightSheetViewer";

   @Override
   public void setContext(Studio studio) {
      studio_ = studio;
   }

   @Override
   public String getSubMenu() {
      return "";
   }

   @Override
   public String getCopyright() {
      return COPYRIGHT_NOTICE;
   }

   @Override
   public String getHelpText() {
      return DESCRIPTION;
   }

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public String getVersion() {
      return VERSION_INFO;
   }

   @Override
   public void onPluginSelected() {
      try {
         SnoutyViewer viewer = new SnoutyViewer(studio_);
         viewer.register();
      } catch (Exception ex) {
         if (studio_ != null) {
            studio_.logs().logError(ex);
         }
      }
   }

   @Override
   public void onPluginSelected(DisplayWindow display) {
      try {
         SnoutyViewer viewer = new SnoutyViewer(studio_, display.getDataProvider());
         viewer.register();
      } catch (Exception ex) {
         if (studio_ != null) {
            studio_.logs().logError(ex);
         }
      }
   }

}

///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//AUTHOR:        Arthur Edelstein, arthuredelstein@gmail.com January 2011
//COPYRIGHT:     University of California, San Francisco, 2011
//LICENSE:       This file is distributed under the BSD license.
//               License text is included with the source distribution.
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.internal.utils;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.micromanager.ApplicationSkin;
import org.micromanager.ApplicationSkin.SkinMode;
import org.micromanager.UserProfile;
import org.micromanager.internal.MMStudio;

public final class FileDialogs {

   public static class FileType {
      final String name;
      final String[] suffixes;
      final String description;
      final boolean suggestFileOnSave;
      String defaultFileName;

      public FileType(String name, String description, String defaultFileName,
                      boolean suggestFileOnSave, String... suffixes) {
         this.name = name;
         this.description = description;
         this.suffixes = suffixes;
         this.defaultFileName = defaultFileName;
         this.suggestFileOnSave = suggestFileOnSave;
      }
   }

   public static final FileType MM_CONFIG_FILE = new FileType("MM_CONFIG_FILE",
         "Micro-Manager Config File", "./MyScope.cfg", true, "cfg");

   public static final FileType MM_DATA_SET = new FileType("MM_DATA_SET",
         "Micro-Manager Image Location", System.getProperty("user.home") + "/Untitled",
         false, (String[]) null);

   public static final FileType SCIFIO_DATA = new FileType("SciFIO_Data_Set",
         "Image Location", System.getProperty("user.home") + "/Untitled.tif",
         false, "tif", "jpg", "avi", "png", "jpg");

   // The first suffix is the one appended when saving; "txt" is kept so that
   // settings files written before the ".mda" suffix was introduced remain visible.
   public static final FileType ACQ_SETTINGS_FILE = new FileType(
         "ACQ_SETTINGS_FILE",
         "Acquisition settings (*.mda, *.txt)",
         System.getProperty("user.home") + "/AcqSettings.mda",
         true, "mda", "txt");

   private static class GeneralFileFilter
         extends javax.swing.filechooser.FileFilter
         implements java.io.FilenameFilter {
      private final String fileDescription_;
      private final String[] fileSuffixes_;

      public GeneralFileFilter(String fileDescription, final String[] fileSuffixes) {
         fileDescription_ = fileDescription;
         fileSuffixes_ = fileSuffixes;
      }

      @Override
      public boolean accept(File pathname) {
         String name = pathname.getName();
         int n = name.lastIndexOf(".");
         // A name without a dot has no suffix at all; substring(1 + n) would
         // otherwise return the whole name and match a file literally named "txt".
         String suffix = n < 0 ? "" : name.substring(n + 1).toLowerCase();
         if (fileSuffixes_ == null || fileSuffixes_.length == 0) {
            return true;
         }
         if (!JavaUtils.isMac() && pathname.isDirectory()) {
            return true;
         }
         for (String s : fileSuffixes_) {
            if (s != null && s.toLowerCase().contentEquals(suffix)) {
               return true;
            }
         }
         return false;
      }

      @Override
      public boolean accept(File dir, String name) {
         return accept(new File(dir, name));
      }

      @Override
      public String getDescription() {
         return fileDescription_;
      }
   }

   /**
    * Appends the file type's primary suffix to a file selected for saving when the
    * name the user typed does not already carry one of the accepted suffixes.
    *
    * <p>Neither AWT's FileDialog nor Swing's JFileChooser does this for us: a
    * FileFilter only decides which files are listed, never how a new one is named.
    * Without this, saving as "AcqSettings7" produces an extension-less file that
    * the very same filter then hides when loading.
    *
    * @param selectedFile file the user chose, may be null.
    * @param fileSuffixes accepted suffixes; the first is the one appended.
    * @return the file with a suffix appended if one was needed.
    */
   private static File ensureSuffix(File selectedFile, final String[] fileSuffixes) {
      if (selectedFile == null || fileSuffixes == null || fileSuffixes.length == 0
            || fileSuffixes[0] == null) {
         return selectedFile;
      }
      // Compare the suffix directly rather than calling filter.accept(): that
      // passes any existing directory through on non-Mac so the chooser stays
      // navigable, which would wrongly suppress the suffix here.
      String name = selectedFile.getName();
      int n = name.lastIndexOf(".");
      String suffix = n < 0 ? "" : name.substring(n + 1).toLowerCase();
      for (String s : fileSuffixes) {
         if (s != null && s.toLowerCase().contentEquals(suffix)) {
            return selectedFile;
         }
      }
      return new File(selectedFile.getAbsolutePath() + "." + fileSuffixes[0]);
   }

   /**
    * Asks the user before overwriting an existing file.
    *
    * <p>Only needed when a suffix was appended after the chooser closed: the file
    * actually written is then not the one the chooser confirmed, so its own
    * overwrite check does not cover the real target.
    *
    * @param parent parent window for the dialog.
    * @param original file as returned by the chooser.
    * @param withSuffix same file after a suffix may have been appended.
    * @return withSuffix, or null if the user declined to overwrite.
    */
   private static File confirmOverwriteIfRenamed(Window parent, File original,
                                                 File withSuffix) {
      if (withSuffix == null || withSuffix.equals(original) || !withSuffix.exists()) {
         return withSuffix;
      }
      int answer = JOptionPane.showConfirmDialog(parent,
            withSuffix.getName() + " already exists.\nDo you want to replace it?",
            "Confirm Save As", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      return answer == JOptionPane.YES_OPTION ? withSuffix : null;
   }

   public static File promptForFile(Window parent,
                                    String title,
                                    File startFile,
                                    boolean selectDirectories, boolean load,
                                    final String fileDescription,
                                    final String[] fileSuffixes,
                                    boolean suggestFileName,
                                    ApplicationSkin skin) {
      File selectedFile = null;
      GeneralFileFilter filter = new GeneralFileFilter(fileDescription, fileSuffixes);

      if (JavaUtils.isMac()) {
         if (selectDirectories) {
            // For Mac we only select directories, unfortunately!
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
         }
         int mode = load ? FileDialog.LOAD : FileDialog.SAVE;
         FileDialog fd;
         if (parent instanceof Dialog) {
            fd = new FileDialog((Dialog) parent, title, mode);
         } else if (parent instanceof Frame) {
            fd = new FileDialog((Frame) parent, title, mode);
         } else {
            fd = new FileDialog((Dialog) null, title, mode);
         }
         if (startFile != null) {
            if (startFile.isDirectory()) {
               fd.setDirectory(startFile.getAbsolutePath());
            } else {
               fd.setDirectory(startFile.getParent());
            }
            if (!load && suggestFileName) {
               fd.setFile(startFile.getName());
            }
         }
         if (fileSuffixes != null) {
            fd.setFilenameFilter(filter);
         }
         fd.setVisible(true);
         if (selectDirectories) {
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
         }
         if (fd.getFile() != null) {
            selectedFile = new File(fd.getDirectory() + "/" + fd.getFile());
            if (mode == FileDialog.SAVE) {
               File original = selectedFile;
               selectedFile = ensureSuffix(selectedFile, fileSuffixes);
               selectedFile = confirmOverwriteIfRenamed(parent, original, selectedFile);
            }
         }
         fd.dispose();

      } else {
         // HACK: we have very limited control over how file choosers are
         // rendered (they're highly platform-specific). Unfortunately on
         // Windows our look-and-feel overrides make choosers look awful in
         // the "night" UI. So we temporarily force the "Daytime" look and
         // feel, without redrawing the entire program UI, just for as long as
         // it takes us to create this chooser.
         skin.suspendToMode(SkinMode.DAY);
         JFileChooser fc = new JFileChooser();
         if (selectDirectories) {
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
         }
         if (startFile != null) {
            if (startFile.isDirectory()) {
               fc.setCurrentDirectory(startFile);
            } else {
               fc.setSelectedFile(startFile);
            }
         }
         skin.resume();
         fc.setDialogTitle(title);
         if (fileSuffixes != null) {
            fc.setFileFilter(filter);
         }
         int returnVal;
         if (load) {
            returnVal = fc.showOpenDialog(parent);
         } else {
            returnVal = fc.showSaveDialog(parent);
         }
         if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            if (!load && !selectDirectories) {
               File original = selectedFile;
               selectedFile = ensureSuffix(selectedFile, fileSuffixes);
               selectedFile = confirmOverwriteIfRenamed(parent, original, selectedFile);
            }
         }
      }
      return selectedFile;
   }

   private static File promptForFile(Window parent, String title,
                                     FileType type, boolean selectDirectories, boolean load,
                                     ApplicationSkin skin) {
      String startFile = getSuggestedFile(type);
      File startDir = null;
      if (startFile != null) {
         startDir = new File(startFile.trim());
      }
      File result = promptForFile(parent, title, startDir, selectDirectories,
            load, type.description, type.suffixes, type.suggestFileOnSave, skin);
      if (result != null) {
         storePath(type, result);
      }
      return result;
   }

   public static void storePath(FileType type, File path) {
      UserProfile profile = MMStudio.getInstance().profile();
      type.defaultFileName = path.getAbsolutePath();
      profile.getSettings(FileDialogs.class).putString(type.name,
            type.defaultFileName);
   }

   public static File openFile(Window parent, String title, FileType type) {
      return promptForFile(parent, title, type, false, true, MMStudio.getInstance().app().skin());
   }

   public static File openDir(Window parent, String title, FileType type) {
      return promptForFile(parent, title, type, true, true, MMStudio.getInstance().app().skin());
   }

   public static File save(Window parent, String title, FileType type) {
      return promptForFile(parent, title, type, false, false, MMStudio.getInstance().app().skin());
   }

   public static String getSuggestedFile(FileType type) {
      return MMStudio.getInstance().profile().getSettings(
            FileDialogs.class).getString(type.name, type.defaultFileName);
   }
}

/*
 * Duplicate an existing project, putting the new files into a subdirectory of the current project.
 *
 * All image analysis is reset, *except* for any TMA cores.  These are kept, along with any metadata.
 *
 * This is particularly useful for generating a new TMA project with the same dearrayed cores & metadata,
 * but otherwise everything kept the same.
 */

import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.panels.ProjectBrowser
import qupath.lib.images.ImageData
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.io.PathIO
import qupath.lib.projects.Project
import qupath.lib.projects.ProjectIO

import java.awt.image.BufferedImage

// Get the running QuPath instance
def qupath = QuPathGUI.getInstance()

// Get the current project
def project = qupath.getProject()
if (project == null) {
    println("No project open!")
    return
}


// Removes all the 'overview' images
for (def entry in project.getImageList()) {
    if ((entry.getImageName() =~ 'overview') || (entry.getImageName() =~ 'label')) {
        project.removeImage(entry)
    }
}

sleep(1000)
getQuPath().refreshProject()
ProjectIO.writeProject(project)
fireHierarchyUpdate()

// Write the new project itself
//ProjectIO.writeProject(projectNew)

print("Done! Project written to ")
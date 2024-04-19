/*
 * Duplicate an existing project, putting the new files into a subdirectory of the current project.
 *
 * All image analysis is reset, *except* for any TMA cores.  These are kept, along with any metadata.
 *
 * This is particularly useful for generating a new TMA project with the same dearrayed cores & metadata,
 * but otherwise everything kept the same.
 */

// Get the running QuPath instance
def qupath = getQuPath()
// Get the current project
def project = getProject()
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
qupath.refreshProject()

fireHierarchyUpdate()
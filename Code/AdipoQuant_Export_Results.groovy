// = CODE DESCRIPTION =
// This script exports the results of an AdipoQuant quantification and can
// be run for the entire project as needed
// Please make sure you have followed the installation instructions at
// https://github.com/Naveiras-Lab/MarrowQuant/tree/qupath-0.1.4
// Code written by Olivier Burri, EPFL-SV-PTECH-BIOP
// For Josefine Tratwal, Naveiras-Lab
// Last update: March 30th 2020



import ch.epfl.biop.qupath.utils.*


selectDetections();
runPlugin('qupath.lib.plugins.objects.ShapeFeaturesPlugin', '{"area": false,  "perimeter": true,  "circularity": true,  "useMicrons": true}');

def um = Utils.um
def columns = ["Adipocyte Index", "Parent", "Area "+um+"^2"]

def resultsfolder = buildFilePath(PROJECT_BASE_DIR, "results")
mkdirs( resultsfolder )

def resultsfile = new File(resultsfolder, "adipocyte-measurements.txt")
println(resultsfile.getAbsolutePath())

def detections = getDetectionObjects()
Utils.sendResultsToFile(columns, detections,  resultsfile)

println("Completed")
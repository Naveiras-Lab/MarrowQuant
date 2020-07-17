// = CODE DESCRIPTION =
// This script exports the results of an MarrowQuant quantification and can
// be run for the entire project as needed
// Please make sure you have followed the installation instructions at
// https://github.com/Naveiras-Lab/MarrowQuant/tree/qupath-0.1.4
// Code written by Olivier Burri, EPFL-SV-PTECH-BIOP
// For Josefine Tratwal, Naveiras-Lab
// Last update: March 30th 2020


// Export results from one or many files to a txt file




def tissues = getAnnotationObjects().findAll {it.getPathClass() == getPathClass('Tissue')}
if( tissues.size() == 0) {
    tissues = getAnnotationObjects().findAll {it.getPathClass() == getPathClass('Tissue Boundaries')}
}


// Create Output Path
outputPath = buildFilePath( PROJECT_BASE_DIR, 'results' )
// Make directory in case it does not exist
mkdirs( outputPath )

// Give the results file a name
fileName = 'MarrowQuant_Results.txt'
delimiter = '\t'
// Write results in the desired directory to the desired filename
file = new File(outputPath, fileName)

def U = 'um'
def columns = ["Image Name", "Tissue Number", 
    "Area Hematopoietic ["+U+"^2]",
    "Area Adipocytes ["+U+"^2]",
    "Area IMV (interstitium and microvasculature) ["+U+"^2]",
    "Area Bone ["+U+"^2]",
    "Area of Total Tissue ["+U+"^2]",
    "Area Of Artifacts ["+U+"^2]",
    "Area Unassigned ["+U+"^2]",
    "% Cellularity (Hematopoietic area/(Marrow area))",
    "% Adiposity (Adipocytic area/(Marrow area))",
    "% IMV area (Interstitium & microvasculature/(Marrow area))",
    "% Unassigned area(Unassigned/(Marrow area))",
    "% Ratio hemato/(hemato+adipo)",
    "Min Size",
    "Max Size",
    "Min Circularity",
    "Total Adipocytes",
    "Num Detections",
    "Num Adipocyte"]

Utils.sendResultsToFile( columns, tissues, file )

import ch.epfl.biop.qupath.utils.*


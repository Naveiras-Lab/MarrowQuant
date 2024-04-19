/* = CODE DESCRIPTION =
 * Export Per-Tissue Boundaries results
 * 
 * == INPUTS ==
 * Teh open image should be from a previously run MarrowQuant2.0
 * 
 * == OUTPUTS ==
 * This script creates a file 'MarrowQuant_Results.txt' with all required statistics
 *
 * = DEPENDENCIES =
 * You need to install the BIOP Tools Extension https://github.com/BIOP/qupath-extension-biop
 * 
 * = INSTALLATION = 
 * After you have installed the extension, nothing else is needed
 * 
 * = AUTHOR INFORMATION =
 * Code made by Olivier Burri, EPFL - SV - PTECH - BIOP 
 * for Rita Sarkis, UPNAVEIRAS
 * 2020.01.20
 * 
 * = COPYRIGHT =
 * Due to the simple nature of this code, no copyright is applicable
 */
 
import qupath.ext.biop.utils.*
// Export results from one or many files to a txt file

def tissues = getAnnotationObjects().findAll { it.getPathClass() == getPathClass( 'Tissue' ) }
if( tissues.size() == 0) {
    tissues = getAnnotationObjects().findAll { it.getPathClass() == getPathClass( 'Tissue Boundaries' ) }
}


// Create Output Path
def outputPath = buildFilePath( PROJECT_BASE_DIR, 'results' )
// Make directory in case it does not exist
mkdirs( outputPath )

// Give the results file a name
def fileName = 'MarrowQuant_Results_20230721_final.txt'

// Write results in the desired directory to the desired filename
def file = new File( outputPath, fileName )

def U = 'um'

// These columns are supposed to match the values in the Tissue Boundaries annotation
def columns = [ "Image Name", 
                "Sample Number", 
                "T.Bd.Ar_"+U+"^2",
                "Art.Ar_"+U+"^2",
                "B.Ar_"+U+"^2",
                "Hm.Ar_"+U+"^2",
                "Tt.Ad.Ar_"+U+"^2",
                "It.Ar_"+U+"^2",
                "Ma.Ar_"+U+"^2",
                "Un.Ar_"+U+"^2",
                "Hm.Ar/(Hm.Ar+Tt.Ad.Ar)_%_Equation_1",
                "Hm.Ar/Ma.Ar_%_Equation_2",
                "Tt.Ad.Ar/Ma.Ar_%",
                "It.Ar/Ma.Ar_%",
                "Un.Ar/Ma.Ar_%",
                "Aj.Ad.N",
                "Ad_Min_Size",
                "Ad_Max_Size", 
                "Ad_Min_Circularity",
                "Num Adipocyte",
                "Ad.MA.Ar_Nby_"+U+"^2"
                ]

Results.sendResultsToFile( columns, tissues, file )
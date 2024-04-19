// = CODE DESCRIPTION =
// This script aims to quantify adipocytes from H&E stained
// bone sections. 
// 
// == INPUTS ==
// To run this code, you need to create multiple annotations in QuPath:
// 1. Annotation of class "Tissue Boundaries" which will contain the 
//    region to analyze.
// 2. Rectangle of class "BG" containing background-only for color
//    balancing
// 3. 1 or more Annotations called "Artifact" marking areas inside 
//    "Tissue Boundaries" that should be excluded from quantification.
// 
// == OUTPUTS ==
// The code produces multiple detections to illustrate the detected areas
// the Detections are called Adipocytes.
//
// = DEPENDENCIES =
// AdipoQuant needs several jars to run properly. Please see the
// installation instruction on GitHub: 
// https://github.com/Naveiras-Lab/MarrowQuant/tree/qupath-0.1.4
// 
// 
// = AUTHOR INFORMATION =
// Code initially written by Ibrahim Bekri, EPFL-SV-GRNAVEIRAS
// Code debugging, supervision and upgrade by Olivier Burri & Rita Sarkis
// EPFL-SV-PTECH-BIOP, EPFL-SV-GRNAVEIRAS
// for Josefine Tratwal, Naveiras-Lab
// DATE
// 
// = COPYRIGHT =
// © All rights reserved. ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP), 2018
// 
// Licensed under the BSD-3-Clause License:
// Redistribution and use in source and binary forms, with or without modification, are permitted provided 
// that the following conditions are met:
// 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
//    in the documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products 
//     derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
// BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
// IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// Parameters to set 
guiscript=true

// Default inputs
def aadipMin = 300 
def aadipMax = 15000
def aminCir = 0.0
def aexcludeOnEdges = true
def adownsample = 2


clearDetections()

def tb = getPathClass("Tissue Boundaries", getColorRGB(0,255,255))
def ar = getPathClass("Artifact", getColorRGB(0,255,255))
def adip_class = getPathClass("Adipocyte", getColorRGB(0,255,255))

def mq_classes = [tb, ar, adip_class]

def all_classes = getQuPath().getAvailablePathClasses().toList()

mq_classes.each { the_class ->
    if (all_classes.find{ it == the_class } != null ) {
        println("Class "+the_class+" already exists")
    } else {
        all_classes.add(the_class)
        println("Creating class "+the_class)
    }
}    

Platform.runLater{ getQuPath().getAvailablePathClasses().setAll(all_classes) }
fireHierarchyUpdate()

def ij = IJExtension.getImageJInstance()

ij.show()
def ad = new AdipocyteDetector()

ad.with {
    downsample =  adownsample
    adipMin = aadipMin*downsample
    adipMax =  aadipMax*downsample
    minCir =  aminCir
    excludeOnEdges =  aexcludeOnEdges
    
}

def tissues = getAnnotationObjects().findAll{ it.getPathClass().equals( getPathClass("Tissue Boundaries") ) }

tissues.eachWithIndex{ tissue, i ->
   
    tissue.setLocked(true)
    println(sprintf("Analysing Tissue Boundaries #%d", i) )
    ad.run(tissue)
}

fireHierarchyUpdate()	
def imageName = getCurrentImageNameWithoutExtension()
println("Processing complete for " +  imageName );
//ij.quit()

// Main class for Adipocyte Detection
class AdipocyteDetector {
    def adipMin = 1000   
    def adipMax = 50000
    def minCir = 0.0
    def downsample = 2
    
    public void run(def tissue) {
        Interpreter.batchMode = true 
        IJ.run("Close All")
        
        // Returns the image as an ImageJ ImagePlus object with ROIs and the overlay
        def request = RegionRequest.createInstance( getCurrentServerPath(), this.downsample, tissue.getROI() )
        def pathImage = IJExtension.extractROIWithOverlay(getCurrentServer(), tissue, getCurrentHierarchy(), request, true, getCurrentViewer().getOverlayOptions());		

        def image = pathImage.getImage()

        // Pick up pixel size
        def px_size = image.getCalibration().pixelWidth
        
        def tissue_roi = image.getRoi()
        
        image.show()
        
        // Extract ROIs
        def overlay = image.getOverlay()
        
        def rois = overlay == null ? null :  overlay.toArray() as List
   
        def artifacts = rois.findAll{ it.getName() =~ /Artifact/ } // will always send a List, could be empty but never null
        
        IJ.log(""+artifacts)
        // Merge all artifacts together.
        def all_edges = uglyArtifactMerge(artifacts, tissue_roi, image)
        
        // Creates HSB stacks from the original image
        def hsb_image = image.duplicate()
        IJ.run(hsb_image, "HSB Stack", "")
        hsb_image.show()
        
        // Call Color Deconvolution and recover the images )
        def cols = colorDeconvolution( image, "H&E" )
                
        // Creates the final image we are going to process from the hue and brightness image obtained
        def ic = new ImageCalculator()
        def adip_raw_image = ic.run("Subtract create", cols[2], cols[0])
        
        adip_raw_image.show()
        
        image.close()
        cols.each{ it.close() }
        
        def saturation = hsb_image.getStack().getProcessor(2) // Saturation is the second image
        saturation.multiply(8)
        
        ic.run("Add", adip_raw_image, new ImagePlus("TesT", saturation) )
        
        adip_raw_image.show()
        
        IJ.setRawThreshold(adip_raw_image, 0, 127, null);
        
        // Morphological operations and analyze particle
        def adip_mask = new ImagePlus("Adip Mask", adip_raw_image.getProcessor().createMask()) // IB?
        IJ.log( ""+adip_mask.isInvertedLut() )
        if( adip_mask.isInvertedLut() ) adip_mask.getProcessor().invertLut()
        IJ.run( adip_mask, "Invert", "") // IB?
        IJ.run( adip_mask, "Watershed", "" )
        adip_mask.show()

                
        IJ.run( adip_mask, "Options...", "iterations=50 count=5 pad do=Erode" )


        adip_mask.setRoi( all_edges )
        //IJ.run(adip_mask, "Make Inverse", "")
        IJ.setRawThreshold(adip_mask, 0, 127, null)
        IJ.run(adip_mask, "Analyze Particles...", "size="+this.adipMin+"-"+this.adipMax+" circularity="+this.minCir+"-1.00 show=Nothing exclude add display")
        
        // Merge all adips as a single selection
        def rm = RoiManager.getInstance() ?: new RoiManager()
        // Save as Detections in QuPath
        def adips = rm.getRoisAsArray() as List
        rm.reset()
        rm.close()
        
        def um = GeneralTools.micrometerSymbol()
        
        def total_area = 0

	// Measurement of adipocytes areas and displaying of the data in QuPath
        adips.eachWithIndex{ adip, idx ->
            def det = IJTools.convertToDetection( adip, this.downsample, image )
            det.setPathClass( getPathClass("Adipocyte") )
            def area = adip.getStatistics().area
            det.getMeasurementList().putMeasurement( "Adipocyte Index", idx+1 )
            det.getMeasurementList().putMeasurement( "Area "+um+"^2", area *  px_size *  px_size )
            
            tissue.addChildObject(det)
            total_area += area
        }
        
        tissue.getMeasurementList().clear()
        tissue.getMeasurementList().putMeasurement( "Total Adipocyte Area "+um+"^2", total_area *  px_size *  px_size )       
        Interpreter.batchMode = false
        fireHierarchyUpdate()
    }
        
	// Excludes the artifacts from the ROI we want to process
    private Roi uglyArtifactMerge(def artifacts, def tissue_roi, def image) {
        if ( artifacts.isEmpty() ) { 
            return tissue_roi 
        }
        
        if (artifacts.size() > 0) {
            
            def rm = RoiManager.getInstance() ?: new RoiManager()
            rm.reset()    
            artifacts.each{ rm.addRoi(it) }
            def all_artifacts
            if ( artifacts.size() == 1 ) {
                all_artifacts = artifacts[0]
            } else {
                rm.setSelectedIndexes((0..rm.getCount()-1) as int[])
                rm.runCommand(image, "OR")
                all_artifacts = image.getRoi()
            }
            rm.reset()
            // AND then XOR with tissue
            IJ.log(""+all_artifacts)
            rm.addRoi(all_artifacts)
            rm.addRoi(tissue_roi)
            rm.setSelectedIndexes([0,1] as int[])
            rm.runCommand(image, "AND")
            def overlap_artifacts = image.getRoi()
            rm.addRoi( overlap_artifacts )
            rm.setSelectedIndexes([1,2] as int[])
            rm.runCommand(image, "XOR")
            rm.reset()
            rm.close()
            return image.getRoi()
        }
    }
    
    // Use Color Deconvolution Plugin in MarrowQuant
    public ImagePlus[] colorDeconvolution ( ImagePlus image, String stain ) {
            def cd = new Colour_Deconvolution()
            def matList = cd.getStainList()
            def mt = matList.get( stain )
            def stackList = mt.compute( false, true, image )
            // This returns an array of ImageStacks
            
            // Make into an ImagePlus            
            def imageStack = new ImageStack( stackList[0].getWidth(), stackList[0].getHeight() )
            
            stackList.each { imageStack.addSlice( it.getProcessor( 1 ) ) }
            
            def deconvolved = stackList.collect{ new ImagePlus( image.getTitle()+"-"+stain, it ) }
            
            return deconvolved      
    }
}


import ij.IJ
import sc.fiji.colourDeconvolution.*
import ij.WindowManager
import ij.plugin.ImageCalculator
import ij.ImagePlus
import ij.process.ImageProcessor
import ij.plugin.frame.RoiManager
import qupath.lib.objects.PathDetectionObject
import ij.macro.Interpreter
import ij.ImageStack
import ij.gui.Roi
import qupath.imagej.gui.IJExtension

import qupath.imagej.helpers.*
import qupath.lib.objects.PathAnnotationObject




	
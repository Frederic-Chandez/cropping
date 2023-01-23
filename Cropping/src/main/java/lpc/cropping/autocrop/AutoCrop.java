package lpc.cropping.autocrop;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// LOG4J
//import org.apache.logging.log4j.*;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import ij.plugin.ContrastEnhancer;
import ij.plugin.GaussianBlur3D;
import ij.process.AutoThresholder;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;
import ij.process.StackConverter;
import ij.process.StackStatistics;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions; // Helper class for managing Bio-Formats Importer options
import lpc.cropping.plugins.Cropping_;
import loci.common.RandomAccessInputStream;
import loci.common.xml.XMLTools;
import loci.formats.CoreMetadata;
import loci.formats.MetadataTools;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.meta.MetadataStore;
import loci.formats.tiff.IFD;
import loci.formats.tiff.PhotoInterp;
import loci.formats.tiff.TiffParser;
import loci.formats.ClassList;
import loci.formats.in.LeicaSCNReader;


public class AutoCrop {
	/** Logger SLF4J */
	//private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final Logger LOGGER = LoggerFactory.getLogger(Cropping_.class);
	
	/** Logger LOG4J */
	
	private final AutocropParameters autocropParameters; /* Parameters crop analyse */
	private final String 	outputDirPath; /* the path of the directory where are saved the crop of the object */
	private ImagePlus       rawImg; /* Raw image */	
	private ImagePlus[]     currentImage; /* Current image for BF */
	private ImagePlus 		imageSeg; /* Segmented image */
	private String          imageFilePath; /* The path of the image to be processed */
	File 					currentFile; /* File to process (Image input) */
	int 					channelNumbers = 1; /* Number of channels in current image */	
	
	/* Classe AutocropParameters */
	int     				channelToComputeThreshold = 0; /* Channel to compute OTSU threshold */	
	private int     		thresholdOTSUComputing = 20;/* Minimal default OTSU threshold */
	
	/**
	 * Autocrop constructor : initialisation of analyse parameter
	 *  - imageFile                 Current image analyse
	 *  - autocropParametersAnalyse List of analyse parameter
	 */
	public AutoCrop(File imageFile, AutocropParameters autocropParametersAnalyse)
	throws IOException, FormatException { // loci exception

		System.out.println("AutoCrop constructor(Input image file) : " + imageFile);
		
		LOGGER.debug("msg de debogage");
	    LOGGER.info("msg d'information");
	    LOGGER.warn("msg d'avertissement");
	    LOGGER.error("msg d'erreur");
	    LOGGER.trace("msg de trace"); 
	    
        LOGGER.debug("Hello from Logback");
		   
		// Disable DEBUG logging of BioFormats writer
		// logging level to INFO, WARN or ERROR
		//DebugTools.setRootLevel("OFF");

/* AutoCrop.java NucleusJ2
 * 
 * 	public AutoCrop(File imageFile, String outputFilesPrefix, AutocropParameters autocropParametersAnalyse)
	throws IOException, FormatException {
        this.autocropParameters = autocropParametersAnalyse;
		this.currentFile = imageFile;
		this.imageFilePath = imageFile.getAbsolutePath();
		this.outputDirPath = this.autocropParameters.getOutputFolder();
		this.outputFilesPrefix = outputFilesPrefix;
		setChannelNumbers();
		if (this.rawImg.getBitDepth() > 8) {
			this.imageSeg =
				Thresholding.contrastAnd8bits(getImageChannel(this.autocropParameters.getChannelToComputeThreshold()));
		} else {
			this.imageSeg = 
				getImageChannel(this.autocropParameters.getChannelToComputeThreshold());
		}
		this.infoImageAnalysis = autocropParametersAnalyse.getAnalysisParameters();
	}
*/	
		this.autocropParameters = autocropParametersAnalyse;
		this.currentFile = imageFile;
		this.imageFilePath = imageFile.getAbsolutePath();
		this.outputDirPath = this.autocropParameters.getOutputFolder();
		
		// Use a dialog box with ImageJ
		//ImagePlus imp = IJ.openImage(this.imageFilePath);
		//ImagePlus imp = new Opener().openImage(this.imageFilePath);

        // Information about the image : Detection Number of channels in this images.
		currentImage = openBFImagePlus(this.imageFilePath); // BF.openImagePlus

		// setChannelNumbers() : check multichannel and initialising channelNumbers variable	
		// length > 1 : c0,c1,...
		// Cette ligne remplace la methode "setChannelNumbers()" : calcule du nb de canaux dans l'image
		int channelNumbers = ChannelSplitter.split(currentImage[0]).length; // separe les canaux de la 1ere image de la stack

        this.rawImg = currentImage[0];
		int width = this.rawImg.getWidth();
	    int height = this.rawImg.getHeight();
	    int nslices = this.rawImg.getStackSize();
	    String imtitle = this.rawImg.getTitle();

	    System.out.print("AutoCrop constructor option(setAutoscale) inputFile : ");
		System.out.println(imtitle + " " + width + " " + height + " " + nslices);

		System.out.println("AutoCrop constructor bitDepth   = " + this.rawImg.getBitDepth());
		System.out.println("AutoCrop constructor NdOfDims   = " + this.rawImg.getNDimensions());
		System.out.println("AutoCrop constructor Type       = " + this.rawImg.getType());
		System.out.println("AutoCrop constructor NbSlices   = " + this.rawImg.getNSlices());
		System.out.println("AutoCrop constructor NbFrames   = " + this.rawImg.getNFrames());
		System.out.println("AutoCrop constructor NbChannels = " + channelNumbers);

		if (this.rawImg.getType() != ImagePlus.GRAY8) {
		  // In order to support 32bit images, pict[] must be changed to float[], and  getPixel(x, y);
	      IJ.error("8 bit greyscale image required. Image requires a 8-bit conversion.");
	    } else
			IJ.error(imtitle+" is a 8 bit greyscale image. No conversion needed.");

		this.rawImg.show();

		// Ouverture sans bio-format (ImageJ)
		getImage().show();

		if (this.rawImg.getNDimensions() > 3) { // Nb de dimensions (2,3,4,5)
		    IJ.error("3D stacks are currently not supported");
		    return;
		} else
			IJ.error(imtitle+" is a accepted 3D stack.");

		if (currentImage[0].isComposite()) { // image multi-canaux
		    int z = currentImage[0].getSlice();
		    System.out.println("Number of Channels = " + z);
		    }
		else
			IJ.error("Split Channels", "Multichannel image required");
		
		/* suite AutoCrop constructeur */ 
		System.out.println("Canal a utiliser pour seuiller l'image = " + channelToComputeThreshold);
		
		if (this.rawImg.getBitDepth() > 8) {
			System.out.println("ImgSeg = getImage(Thresholding.contrastAnd8bits(ChannelToComputeThreshold))");
			//this.imageSeg =
			//	Thresholding.contrastAnd8bits(getImageChannel(this.autocropParameters.getChannelToComputeThreshold()));
			this.imageSeg = contrastAnd8bits(currentImage[channelToComputeThreshold]);
		} else {
			System.out.println("ImgSeg = getImage(ChannelToComputeThreshold)");
			//this.imageSeg = 
			//	getImageChannel(this.autocropParameters.getChannelToComputeThreshold());
			this.imageSeg = currentImage[channelToComputeThreshold];
		}
		
		/** AutoCropCalling.Suite runFile()
		 * AutoCrop.thresholdKernels();
		 * Realise un seuillage OTSU + Segmentation 
		 */
		thresholdKernels();
	}
	
	/**
	 * Method computing OTSU threshold and creating segmented image from this threshold. 
	 * Before OTSU threshold a Gaussian Blur is applied (case of anisotropic voxels)
	 * 
	 * TODO : add case where voxel are not anisotropic for Gaussian Blur Case where OTSU threshold 
	 * is under 20 computation using only half of last slice (useful in case of top slice with lot 
	 * of noise) If OTSU threshold is still under 20 threshold default threshold value is 20.
	 */	
	public void thresholdKernels() {
		LOGGER.info("Thresholding kernels.");
		if (this.imageSeg == null) {
			return;
		}
		System.out.println("thresholdKernels()");
		GaussianBlur3D.blur(this.imageSeg, 0.5, 0.5, 1);
		// Autothresholding methods
		//  Calculates and returns a threshold using the specified method and 256 bin histogram. 
		//int thresh = Thresholding.computeOTSUThreshold(this.imageSeg);
		AutoThresholder autoThresholder = new AutoThresholder();
		ImageStatistics imageStatistics = new StackStatistics(this.imageSeg);
		int[]           tHistogram      = imageStatistics.histogram;
		int thresh = autoThresholder.getThreshold(AutoThresholder.Method.Otsu, tHistogram);
		
		System.out.println("Thresholding kernels ImgSeg Threshold = " + thresh);
	
		/*
		if (thresh < this.autocropParameters.getThresholdOTSUComputing()) {
			ImagePlus imp2;
			if (autocropParameters.getSlicesOTSUComputing() == 0) {
				this.sliceUsedForOTSU =
						"Start:" + this.imageSeg.getStackSize() / 2 + "-" + this.imageSeg.getStackSize();
				imp2 = new Duplicator().run(this.imageSeg,
				                            this.imageSeg.getStackSize() / 2,
				                            this.imageSeg.getStackSize());
			} else {
				this.sliceUsedForOTSU = "Start:" +
				                        this.autocropParameters.getSlicesOTSUComputing() +
				                        "-" +
				                        this.imageSeg.getStackSize();
				imp2 = new Duplicator().run(this.imageSeg,
				                            this.autocropParameters.getSlicesOTSUComputing(),
				                            this.imageSeg.getStackSize());
			}
			int thresh2 = Thresholding.computeOTSUThreshold(imp2);
			if (thresh2 < this.autocropParameters.getThresholdOTSUComputing()) {
				thresh = this.autocropParameters.getThresholdOTSUComputing();
				this.defaultThreshold = true;
			} else {
				thresh = thresh2;
			}
		}
		this.otsuThreshold = thresh;
		this.imageSeg = this.generateSegmentedImage(this.imageSeg, thresh);
		*/
	}

	/**
	 * TODO COMMENT !!!! 2D 3D
	 *
	 * @param imagePlusInput
	 *
	 * @return
	 */
	public ImagePlus contrastAnd8bits(ImagePlus imagePlusInput) {
		/* Thresholding.contrastAnd8bits */ 
		System.out.println("Thresholding.contrastAnd8bits");
		
		ContrastEnhancer enh = new ContrastEnhancer();
		enh.setNormalize(true);
		enh.setUseStackHistogram(true);
		enh.setProcessStack(true);
		enh.stretchHistogram(imagePlusInput, 0.05);
		StackStatistics statistics = new StackStatistics(imagePlusInput);
		imagePlusInput.setDisplayRange(statistics.min, statistics.max);
		
		if (imagePlusInput.getNSlices() > 1) { // 3D
			System.out.println("Thresholding.contrastAnd8bits STACK CONVERTER -> GRAY8");
			StackConverter stackConverter = new StackConverter(imagePlusInput);
			stackConverter.convertToGray8();
		} else { // 2D
			System.out.println("Thresholding.contrastAnd8bits IMAGE CONVERTER -> GRAY8");
			ImageConverter imageConverter = new ImageConverter(imagePlusInput);
			imageConverter.convertToGray8();
		}
		return imagePlusInput;		
	}
	
	/**
	 * Method to check multichannel and initialising channelNumbers variable
	 */
	public ImagePlus[] openBFImagePlus(String path) {
		ImagePlus[] imps = null;
		// Bio-Formats ImporterOptions controle les otions d'ouverture des images
		System.out.println("Bio-Formats ImporterOptions");
		
	    try {
	     System.out.println("Trying LOCI Import Image in AutoCrop constructor");
	     // Bio-Formats Import Options
	     ImporterOptions options = new ImporterOptions();
	     //options.setId(this.imageFilePath);
	     options.setId("‪C:\\Users\\frede\\Desktop\\2013-01-18-crwn12-102_GC1.tif");
	     options.setAutoscale(false);
	     //options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
	     //options.setStackOrder(ImporterOptions.ORDER_XYCZT);
	     //options.setColorMode(ImporterOptions.COLOR_MODE_DEFAULT);
	     //options.setColorMode(ImporterOptions.COLOR_MODE_COMPOSITE);
	     //options.setCrop(true);
	     //options.setVirtual(virtual);
	     //options.setGroupFiles(true);
	     //imps = BF.openImagePlus(options); // ou BF.openImagePlus(this.imageFilePath);
	     imps = BF.openImagePlus(this.imageFilePath);
	    }
	    catch (FormatException e) {
	    	IJ.error("Sorry, an error occurred: " + e.getMessage());
		}
		catch (IOException e) {
			IJ.error("Sorry, an error occurred: " + e.getMessage());
		}
	    return imps;
	}
	
	/**
	 * Method to get specific channel to compute OTSU threshold
	 * @param channelNumber Number of channel to compute OTSU for crop
	 * @return image of specific channel
	 */
	public ImagePlus getImageChannel(int channelNumber) throws IOException, FormatException {
		ImagePlus[] currentImage = BF.openImagePlus(this.imageFilePath);
		currentImage = ChannelSplitter.split(currentImage[0]);
		return currentImage[channelNumber];
	}
	
	/**
	 * AutoCrop.java : Charger une image sans bio-format
	 */
	public ImagePlus getImage() throws IOException {
		ImagePlus currentImage = IJ.openImage(this.imageFilePath);
		return currentImage;
	}
}

/*
 // Image Bit-depth must be 8-bit
 if (this.rawImg.getBitDepth() > 8) { // Returns the bit depth, 8, 16, 24 (RGB) or 32.
 	 throw new IllegalArgumentException("Unsupported conversion");
 } else { // 2D only
	ImageConverter imageConverter = new ImageConverter(rawImg);
	imageConverter.convertToGray8();
 }
*/

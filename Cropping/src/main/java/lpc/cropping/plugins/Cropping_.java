package lpc.cropping.plugins;

import java.io.File;
import java.lang.invoke.MethodHandles;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.IJ;
import ij.plugin.PlugIn;
import lpc.cropping.autocrop.AutoCrop;
import lpc.cropping.autocrop.AutocropParameters;

public class Cropping_ implements PlugIn {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private String input;
	private String output;
	private AutocropParameters autocropParameters;

	@Override
	public void run(String args) {
		if (IJ.versionLessThan("1.32c")) {
			return;
		}
		IJ.showMessage("Autocrop Tester en Java pour ImageJ !");
		IJ.showStatus("Plugin Test started.");

		LOGGER.info("run() Demarrage du plugin Autocrop");

		// Dialogue :
		//  - selection d'une image input
		//  - selection d'un directory output

	    JFileChooser jfc = new JFileChooser( // JFileChooser(String directoryPath) utilise le chemin donne
	            FileSystemView
	            .getFileSystemView()
	            .getHomeDirectory()
	        );

	    // l'utilisateur verra seulement les images TIFF
	    FileNameExtensionFilter filter = new FileNameExtensionFilter(
	            "Tiff Images", "tif", "tiff");
	    jfc.setFileFilter(filter);
	    // pas de filtre
	    //jfc.setAcceptAllFileFilterUsed(false);

		// Input image file ...
	    jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);

	    // Pops up an "Open File" file chooser dialog
	    if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
	    	System.out.println("Source approuvee!");
			File selectedInput = jfc.getSelectedFile();
			input = selectedInput.getPath();
		     System.out.println("You chose to open this file: " + input); // jfc.getSelectedFile().getName());
	    } else
	    	System.out.println("Source rejetee!");
/*
		// Output directory ...
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			// try-catch block to handle exceptions
	        try {
				File selectedOutput = jfc.getSelectedFile();
	            // Display the file path/file path of Canonical of the file object
	            System.out.println("Original file path : " + selectedOutput.getPath());
	            System.out.println("Canonical file path : " + selectedOutput.getCanonicalPath());
	        }
	        catch (Exception e) {
	            System.err.println(e.getMessage());
	        }
		}
*/
		jfc.setSelectedFile(null);

		validate();

		LOGGER.info("run() FIN run Autocrop");
	}

	// Verification/enregistrement des fichiers I/O
	// Recuperation des parametres de traitement
	private void validate() {

		//String input  = autocropDialog.getInput();  // input file
		//String input  = new String("C:/Users/frede/OneDrive/Desktop/WT-W1.TIF");  // input file

		//String output = autocropDialog.getOutput(); // output directory path
		//String output = new String("C:/Users/frede/OneDrive/Desktop/WT-W1-Output");  // output file

		System.out.println("Input image file : " + input);
		System.out.println("Output directory : " + output);

		if (input == null || input.equals("")) {
			IJ.error("Input file is missing");
		/*
		} else if (output == null || output.equals("")) {
			IJ.error("Output directory is missing");
			*/
		} else {
			System.out.println("------> bloc Try");
			try {				
				LOGGER.info("Validation des saisies utilisateur...");
				// l'objet autocropParameters stocke tous les parametres utiles au t/t de l'image
				autocropParameters = new AutocropParameters(input, output);
				File file = new File(input);
				System.out.println("Appel autocrop runFile(String rawImage) " + input);
				runFile(input);
				LOGGER.info("Autocrop process has ended successfully");
			} catch (Exception e) {
				LOGGER.error("An error occurred during autocrop.", e);
			}
		}
	}

	public void runFile(String inputImage) {
		System.out.println("Cropping_.runFile()");
		File currentImageFile = new File(inputImage);
		LOGGER.info("Cropping_.runFile() Current file: {}", currentImageFile.getAbsolutePath());
		String fileImg = currentImageFile.toString();
		File outPutFileName = new File(fileImg);
		System.out.println("Cropping_.runFile() trying calling Autocrop constructor...");
		try {
			// creer un objet AutoCrop avec les parametres de tt contenus dans l'objet autocropParameters
			// l'objet autocropParameters recupere toutes les constantes utiles au t/t de l'image
			//   AutocropParameters autocropParameters = new AutocropParameters(input, output);
			//   currentImageFile 	: inputFile ex:"WT-W1.TIF" (AutocropParameters.inputFile contient deja cet info!)
			//   prefix 			: output    ex:"WT-W1-Output" (AutocropParameters.outputFolder contient deja cet info!)
			//   autocropParameters : argument "autocropParameters"
			AutoCrop autoCrop = new AutoCrop(currentImageFile, autocropParameters);
		}
		catch (Exception e) {
			LOGGER.error("An error occurred. Cannot run autocrop on: " + currentImageFile.getName(), e);
			IJ.error("Cannot run autocrop on " + currentImageFile.getName());
		}
	}

	public void showAbout() {
		IJ.showMessage("Test Autocropping", "Modele pour pour traiter une pile d'images TIFF");
	}

	/**
	 * Main method for debugging - For debugging, it is convenient to have a method
	 * that starts ImageJ, loads an image and calls the plugin, e.g. after setting breakpoints.
	 */
	public static void main(String[] args) throws Exception {
		
		// must be set before the first call to  LoggerFactory.getLogger();
	    // ContextInitializer.CONFIG_FILE_PROPERTY is set to "logback.configurationFile"
	    //System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "/path/to/config.xml");
	    
		//Set this before the logger start.
		System.setProperty("filename", "testFile.log");
		System.out.println("System property: " + System.getProperty("user.dir"));
	    System.out.println("Operating System: " + System.getProperty("os.name"));
	    System.out.println("Java runtime version: " + System.getProperty("java.runtime.version" ));
	      
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Cropping_.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// run the plugin
	 	IJ.runPlugIn(clazz.getName(), "");	//IJ.runPlugIn(Cropping_.class.getName(), "");
	}
}

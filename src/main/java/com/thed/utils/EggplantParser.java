package com.thed.utils;
/**
*
* @author Mohan.Kumar
* This parses eggPlant results
*/
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.thed.model.EggPlantResult;

import au.com.bytecode.opencsv.CSVReader;
import com.thed.model.EggPlantScriptLine;

public final class EggplantParser {
    private final String sut;
    private final String url;

    public EggplantParser(String sut, String url)
   {
        this.sut = sut;
        this.url = url;
    }

    public ArrayList<EggPlantResult> invoke(File f)
	{
		List<File> suitefileList = new ArrayList<File>();
		if (f.isDirectory()) {
			suitefileList = (Arrays.stream(f.listFiles()).collect(Collectors.toList())).stream()
					.filter(file -> file.getName().endsWith(".csv")).collect(Collectors.toList());
		} else {
			suitefileList.add(f);
		}

		List<File> testSuiteFol = getTestSuits(suitefileList);

		ArrayList<EggPlantResult> results = new ArrayList<EggPlantResult>();
		testSuiteFol.forEach((file) -> {

			FileReader fr = null;
			CSVReader reader = null;
			try {
				fr = new FileReader(file);
				reader = new CSVReader(fr);
				// skip the header row
				String[] line = reader.readNext();
				// Loop round reading each line
				while ((line = reader.readNext()) != null) {
					EggPlantResult result = new EggPlantResult();
					result.setRunDate(line[0]);
					result.setDuration(line[2]);
					result.setPassed(line[1].equals("Success"));
					result.setErrors(line[3]);
					result.setWarnings(line[4]);
					result.setExceptions(line[5]);
					result.setScript(f.getParent());
					result.setSut(sut);
					result.setXmlResultFile(file.getParent() + "/" + line[6].replaceAll(".txt", ".xml"));
					getResultLines(result, file.getParent() + "/" + line[6], url);

					results.add(result);
				}
			} catch (FileNotFoundException ex) {
				Logger.getLogger(EggplantParser.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(EggplantParser.class.getName()).log(Level.SEVERE, null, ex);
			} catch (Exception ex) {
				Logger.getLogger(EggplantParser.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException ex) {
						Logger.getLogger(EggplantParser.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
				if (fr != null) {
					try {
						fr.close();
					} catch (IOException ex) {
						Logger.getLogger(EggplantParser.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		});
		return results;
	}
    private List<File> getTestSuits(List<File> suitefileList) {
    	List<File> suitList=new ArrayList<File>();
    	List<File> csvFiles=new ArrayList<File>();
    	
    	suitefileList.forEach((file)->{
    		FileReader fr;
    		CSVReader reader = null;
			try {
				fr = new FileReader(file);
				 reader = new CSVReader(fr);
				String[] line = reader.readNext();
				while ((line = reader.readNext()) != null) {
					String suiteName=line[0];
					File suiteFile=new File( file.getParent() + "/" + suiteName);
					suitList.add(suiteFile);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}finally {
				if(reader!=null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
    	});
    	
    	suitList.forEach(f->{
    		File file=(Arrays.stream(f.listFiles()).collect(Collectors.toList())).stream().filter(csvFile->csvFile.getName().endsWith(".csv")).findFirst().get();
    		csvFiles.add(file);
    	});
    	
		return csvFiles;
	}

	/**
     * Reads the individual result lines from an eggPlant results file
     *
     * @param result    parent of the result line
     * @param file      file containing the result lines
    * @param url       url to where images will be stored TODO: Not working
	 * @throws Exception 
     */
    private void getResultLines(EggPlantResult result, String file, String url) throws Exception
    {
        FileReader fr = null;
        CSVReader reader = null;
        File inputFile=new File(file);
        if(inputFile.exists()) {
        	try {
        		// These files are tab separated
        		fr = new FileReader(inputFile);
        		reader = new CSVReader(fr, '\t');
        		// skip the header row
        		String[] line = reader.readNext(); // throw away the header line
        		
        		int step = 1;
        		// Loop round reading each line
        		while ((line = reader.readNext()) != null) {
        			EggPlantScriptLine resultLine = new EggPlantScriptLine();
        			resultLine.setStep(step++);
        			resultLine.setTime(line[0]);
        			resultLine.setMessage(line[1]);
        			resultLine.setImage(line[2]);
        			resultLine.setText(line[3]);
        			resultLine.setImageURL(url + "/" + line[2]);  // TODO : How to get the image!!!!
        			
        			result.addScriptLine(resultLine);
        		}
        		
        	} catch (Exception e) {
        		Logger.getLogger(EggplantParser.class.getName()).log(Level.SEVERE, null, e.getMessage());
        	}
        	finally{
        		if (reader != null)
        		{
        			try {
        				reader.close();
        			} catch (IOException ex) {
        				Logger.getLogger(EggplantParser.class.getName()).log(Level.SEVERE, null, ex);
        			}
        		}
        		if (fr != null)
        		{
        			try {
        				fr.close();
        			} catch (IOException ex) {
        				Logger.getLogger(EggplantParser.class.getName()).log(Level.SEVERE, null, ex);
        			}
        		}
        	}
        }
        }
}
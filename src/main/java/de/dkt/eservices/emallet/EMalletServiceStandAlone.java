package de.dkt.eservices.emallet;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.hp.hpl.jena.rdf.model.Model;

import de.dkt.common.filemanagement.FileFactory;
import de.dkt.common.niftools.NIFReader;
import de.dkt.common.tools.ParameterChecker;
import eu.freme.common.conversion.rdf.JenaRDFConversionService;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFConstants.RDFSerialization;
import eu.freme.common.conversion.rdf.RDFConversionService;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.ExternalServiceFailedException;
import eu.freme.common.rest.BaseRestController;
import eu.freme.common.rest.NIFParameterSet;


@RestController
public class EMalletServiceStandAlone extends BaseRestController{
	
	Logger logger = Logger.getLogger(EMalletServiceStandAlone.class);

	@Autowired
	EMalletService service;
	
	public EMalletServiceStandAlone() {
		System.err.println("xxx");
	}
	
	@RequestMapping(value = "/e-mallet/testURL", method = { RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<String> testURL(
			@RequestParam(value = "preffix", required = false) String preffix,
			@RequestBody(required = false) String postBody) throws Exception {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/plain");
		ResponseEntity<String> response = new ResponseEntity<String>("The restcontroller is working properly", responseHeaders, HttpStatus.OK);
		return response;
	}
		
	@RequestMapping(value = "/e-mallet/trainModel", method = {RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<String> trainModel(
			HttpServletRequest request,
			@RequestParam(value = "trainDataFile", required = false) String trainDataFile,
			@RequestParam(value = "language", required = false) String language,
			@RequestParam(value = "modelName", required = false) String modelName,
			@RequestParam(value = "modelPath", required = false) String modelPath,
			@RequestParam(value = "analysis", required = false) String analysis,
			@RequestBody(required = false) String postBody) throws Exception {
		try {
//			ParameterChecker.checkNotNullOrEmpty(trainDataFile, "trainDataFile");
			ParameterChecker.checkInList(analysis, "topicmodelling;classification", "analysis", logger);
			ParameterChecker.checkNotNullOrEmpty(modelName, "modelName", logger);
			ParameterChecker.checkNotNullOrEmpty(language, "language", logger);
			
	        MultipartFile file = null;//= multipartRequest.getFile("file");
			if (request instanceof MultipartHttpServletRequest){
		           MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		           file = multipartRequest.getFile("trainingFile");
		   		if(file==null){
					logger.error("No file received in request");
					throw new BadRequestException("No file received in request");
				}
		        if (!file.isEmpty()) {
			        trainDataFile="tmpFiles" + File.separator+file.getOriginalFilename();
	        		File tmpFile;
		        	byte[] bytes;
		        	try {
		        		tmpFile = FileFactory.generateOrCreateFileInstance(trainDataFile);
		        	} catch (Exception e) {
//		        		e.printStackTrace();
		        		logger.error("Fail at generating temporary file.");
		        		throw new BadRequestException("Fail at generating temporary file.");
		        	}
		        	try {
		        		bytes = file.getBytes();
		        	} catch (Exception e) {
		        		logger.error("Fail at reading input file.");
		        		throw new BadRequestException("Fail at reading input file.");
		        	}
		        	try {
		        	    FileOutputStream fos = new FileOutputStream(tmpFile); 
		        	    fos.write(bytes);
		        	    fos.close();
		        	} catch (Exception e) {
		        		logger.error("Fail at writting temporary file.");
		        		throw new BadRequestException("Fail at writting temporary file.");
		        	}
		        } else {
		        	logger.error("The given file was empty.");
		        	throw new BadRequestException("The given file was empty.");
		        }
	        }
			else{
	        	logger.error("No training file provided: HTTPREQUEST is no multipart.");
	        	throw new BadRequestException("No training file provided: HTTPREQUEST is no multipart.");
			}
            
			ParameterChecker.checkNotNullOrEmpty(trainDataFile, "trainingDataFile", logger);
			String outString;
			if(analysis.equalsIgnoreCase("topicmodelling")){
				outString = service.trainModelTopic(trainDataFile, modelPath, modelName, language);
			}
			else if(analysis.equalsIgnoreCase("classification")){
				outString = service.trainModelClassification(trainDataFile, modelPath, modelName, language);
			}
			else{
				logger.error("The input analysis ["+analysis+"] is not supported. Only topicmodelling/classification are available.");
				throw new BadRequestException("The input analysis ["+analysis+"] is not supported. Only topicmodelling/classification are available.");
			}
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Content-Type", "text/plain");
    		ResponseEntity<String> response = new ResponseEntity<String>(outString, responseHeaders, HttpStatus.OK);
    		return response;
		} catch (BadRequestException e) {
			logger.error(e.getMessage());
			throw e;
		} catch (ExternalServiceFailedException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}
	
	@RequestMapping(value = "/e-documentclassification", method = {RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<String> documentClassification(
			@RequestParam(value = "input", required = false) String input,
			@RequestParam(value = "i", required = false) String i,
			@RequestParam(value = "informat", required = false) String informat,
			@RequestParam(value = "f", required = false) String f,
			@RequestParam(value = "outformat", required = false) String outformat,
			@RequestParam(value = "o", required = false) String o,
			@RequestParam(value = "prefix", required = false) String prefix,
			@RequestParam(value = "p", required = false) String p,
			@RequestHeader(value = "Accept", required = false) String acceptHeader,
			@RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
            @RequestParam Map<String, String> allParams,

			@RequestParam(value = "language", required = false) String language,
			@RequestParam(value = "modelName", required = false) String modelName,
			@RequestParam(value = "modelPath", required = false) String modelPath,
			@RequestBody(required = false) String postBody) throws Exception {
		try {
			ParameterChecker.checkNotNullOrEmpty(modelName, "modelName", logger);
			ParameterChecker.checkNotNullOrEmpty(language, "language", logger);

			NIFParameterSet nifParameters = this.normalizeNif(input, informat, outformat, postBody, acceptHeader, contentTypeHeader, prefix);
            String textForProcessing = null;
            if (nifParameters.getInformat().equals(RDFConstants.RDFSerialization.PLAINTEXT)) {
                textForProcessing = nifParameters.getInput();
    			//rdfConversionService.plaintextToRDF(inModel, textForProcessing,language, nifParameters.getPrefix());
            } else {
            	
            	if(nifParameters.getInput()!=null){
                    textForProcessing = nifParameters.getInput();
            	}
            	else if(postBody!=null){
                    textForProcessing = postBody;
            	}
                if (textForProcessing == null) {
                    logger.error("BADREQUEST: No text to process.");
                    throw new BadRequestException("No text to process.");
                }
            }
            Model outModel;
            outModel = service.analyzeText(textForProcessing, "documentclassification", modelPath, modelName, language, informat, outformat);
			return createSuccessResponse(outModel, nifParameters.getOutformat());
		} catch (BadRequestException e) {
			logger.error("EXCEPTION: "+e.getMessage());
			throw e;
		} catch (ExternalServiceFailedException e) {
			logger.error("EXCEPTION: "+e.getMessage());
			throw e;
		}
	}
	
	@RequestMapping(value = "/e-documentclassification/models", method = { RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<String> documentclassificationModels(
			@RequestBody(required = false) String postBody) throws Exception {

		File f = FileFactory.generateOrCreateDirectoryInstance("trainedModels" + File.separator + "documentClassification" + File.separator);
		if(f.isDirectory()){
			
			String sFiles = "";
			File [] files = f.listFiles();
			for (File ff: files) {
				sFiles += ff.getName().replaceAll("\\.EXT$", "").substring(ff.getName().indexOf('-')+1) + "\n";
				
			}
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add("Content-Type", "text/plain");
			ResponseEntity<String> response = new ResponseEntity<String>(sFiles, responseHeaders, HttpStatus.OK);
			return response;
		}
		return null;
	}

	@RequestMapping(value = "/e-topicmodelling/models", method = { RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<String> topicModellingModels(
			@RequestBody(required = false) String postBody) throws Exception {
		File f = FileFactory.generateOrCreateDirectoryInstance("trainedModels" + File.separator + "topicModelling" + File.separator);
		if(f.isDirectory()){
			String sFiles = "";
			File [] files = f.listFiles();
			for (File ff: files) {
				if(!ff.getName().contains("Alphabet")){
					sFiles += ff.getName().replaceAll("_TOPIC\\.EXT$", "").substring(ff.getName().indexOf('-')+1) + "\n";
				}
				
			}
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add("Content-Type", "text/plain");
			ResponseEntity<String> response = new ResponseEntity<String>(sFiles, responseHeaders, HttpStatus.OK);
			return response;
		}
		return null;
	}
	
	@RequestMapping(value = "/e-topicmodelling", method = {RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<String> topicModelling(
			@RequestParam(value = "input", required = false) String input,
			@RequestParam(value = "i", required = false) String i,
			@RequestParam(value = "informat", required = false) String informat,
			@RequestParam(value = "f", required = false) String f,
			@RequestParam(value = "outformat", required = false) String outformat,
			@RequestParam(value = "o", required = false) String o,
			@RequestParam(value = "prefix", required = false) String prefix,
			@RequestParam(value = "p", required = false) String p,
			@RequestHeader(value = "Accept", required = false) String acceptHeader,
			@RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
            @RequestParam Map<String, String> allParams,

			@RequestParam(value = "language", required = false) String language,
			@RequestParam(value = "modelName", required = false) String modelName,
			@RequestParam(value = "modelPath", required = false) String modelPath,
			@RequestBody(required = false) String postBody) throws Exception {
		try {
			ParameterChecker.checkNotNullOrEmpty(modelName, "modelName",logger);
			ParameterChecker.checkNotNullOrEmpty(language, "language",logger);

			NIFParameterSet nifParameters = this.normalizeNif(input, informat, outformat, postBody, acceptHeader, contentTypeHeader, prefix);
            String textForProcessing = null;

            if (nifParameters.getInformat().equals(RDFConstants.RDFSerialization.PLAINTEXT)) {
                textForProcessing = nifParameters.getInput();
    			//rdfConversionService.plaintextToRDF(inModel, textForProcessing,language, nifParameters.getPrefix());
            } else {
            	
            	if(nifParameters.getInput()!=null){
                    textForProcessing = nifParameters.getInput();
            	}
            	else if(postBody!=null){
                    textForProcessing = postBody;
            	}
                if (textForProcessing == null) {
                    logger.error("BADREQUEST: No text to process.");
                    throw new BadRequestException("No text to process.");
                }
            }
            Model outModel;
			outModel = service.analyzeText(textForProcessing, "topicmodelling", modelPath, modelName, language, informat, outformat);
			return createSuccessResponse(outModel, nifParameters.getOutformat());
		} catch (BadRequestException e) {
			logger.error("EXCEPTION: "+e.getMessage());
			throw e;
		} catch (ExternalServiceFailedException e) {
			logger.error("EXCEPTION: "+e.getMessage());
			throw e;
		}
	}
}

package it.divincenzo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;

public class ServerService {

    // return supported MIME Types
	protected String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html")) {
			return "text/html";
		} else if (fileRequested.endsWith(".css")) {
			return "text/css";
		} else if (fileRequested.endsWith(".js")) {
			return "text/javascript";
		} else if (fileRequested.endsWith(".jpg")) {
			return "image/jpg";
		} else if (fileRequested.endsWith(".jpeg")) {
			return "image/jpeg";
		} else if (fileRequested.endsWith(".png")) {
			return "image/png";
		} else if (fileRequested.endsWith(".gif")) {
			return "image/gif";
		} else if (fileRequested.endsWith(".json")) {
			return "application/json";
		} else if (fileRequested.endsWith(".xml")) {
			return "application/xml";
		} else {
			return "text/plain";
		}
	}

    protected byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];

		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null)
				fileIn.close();
		}

		return fileData;
	}

    protected void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested, File WEB_ROOT, String FILE_NOT_FOUND) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);

		httpResponse(out, dataOut, 404, content, fileLength, fileData);

		if (JavaHTTPServer.verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}

	protected void fileMoved(PrintWriter out, OutputStream dataOut, String fileRequested, File WEB_ROOT, String FILE_MOVED) throws IOException {
		File file = new File(WEB_ROOT, FILE_MOVED);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);

		httpResponse(out, dataOut, 301, content, fileLength, fileData);
		if (JavaHTTPServer.verbose) {
			System.out.println("File " + fileRequested + " moved permanently");
		}
	}

	/*
	 * Attenzione non stampa la prima riga dell'http response con il codice http (200 / 301 / 501 /404)
	 */
	 protected void httpResponse(PrintWriter out, OutputStream dataOut, int httpCode, String contentMimeType, int fileLength, byte[] fileData) throws IOException {
		
		if(httpCode == 200){
			out.println("HTTP/1.1 200 OK");
		} else if (httpCode == 301){
			out.println("HTTP/1.1 301 File Moved Permanently");
		} else if (httpCode == 501){
			out.println("HTTP/1.1 501 Not Implemented");
		} else if (httpCode == 404){
			out.println("HTTP/1.1 404 File Not Found");
		}
		
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + contentMimeType);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between headers and content, very important !
		out.flush(); // flush character output stream buffer
		// file
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();

	}

}

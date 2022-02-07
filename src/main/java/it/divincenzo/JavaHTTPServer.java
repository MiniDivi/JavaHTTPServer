package it.divincenzo;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

// The tutorial can be found just here on the SSaurel's Blog : 
// https://www.ssaurel.com/blog/create-a-simple-http-web-server-in-java
// Each Client Connection will be managed in a dedicated Thread
public class JavaHTTPServer implements Runnable {

	static final File WEB_ROOT = new File("src/main/resources");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String FILE_MOVED = "301.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	// port to listen connection
	static final int PORT = 8080;

	// verbose mode
	static final boolean verbose = true;

	// Client Connection via Socket Class
	private Socket connect;

	public JavaHTTPServer(Socket c) {
		connect = c;
	}

	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

			// we listen until user halts server execution
			while (true) {
				JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());

				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}

				// create dedicated thread to manage the client connection
				Thread thread = new Thread(myServer);
				thread.start();
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;
		String fileRequested = null;

		try {
			// we read characters from the client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// we get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(connect.getOutputStream());

			// get first line of the request from the client
			String input = in.readLine();
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
			// we get file requested
			fileRequested = parse.nextToken().toLowerCase();

			// we support only GET and HEAD methods, we check
			if (!method.equals("GET") && !method.equals("HEAD")) {
				if (verbose) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}

				// we return the not supported file to the client
				File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				// read content to return to client
				byte[] fileData = readFileData(file, fileLength);

				// we send HTTP Headers with data to client
				httpResponse(out, dataOut, 501, contentMimeType, fileLength, fileData);

			} else {
				// GET or HEAD method
				if (fileRequested.endsWith("/")) {
					// http://localhost:8080/ -> index.html (DEFUALT_FILE)
					fileRequested += DEFAULT_FILE;
				} else if (fileRequested.startsWith("/")) {
					// rimuove / iniziale per recuperare il file
					// /pippo.html -> pippo.html
					fileRequested = fileRequested.substring(1);				
				}

				int fileLength = 0;
				byte[] fileData = null;

				if (fileRequested.endsWith(".json")){
					root deserializedXML = XmlDeserializer();
					String s = JsonSerializer(deserializedXML);
					fileLength = s.length();
					fileData = s.getBytes();
				} else if (fileRequested.endsWith(".xml")) {
					DeserializedJson deserializedJson = JsonDeserializer();
					String s = XmlSerializer(deserializedJson);
					fileLength = s.length();
					fileData = s.getBytes();
				} else {
					// file su file system (statico)
					// se il file non esiste provo a fare redrect
					File file = new File(WEB_ROOT, fileRequested);
					
					if (file.exists() && file.isFile()) {
						fileLength = (int) file.length();
						fileData = readFileData(file, fileLength);
					} else {
						fileMoved(out, dataOut, fileRequested);
						return;
					}
				}

				
				String content = getContentType(fileRequested);

				if (method.equals("GET")) { // GET method so we return content
					// send HTTP Headers
					httpResponse(out, dataOut, 200, content, fileLength, fileData);
				}

				if (verbose) {
					System.out.println("File " + fileRequested + " of type " + content + " returned");
				}

			}

		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}

		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}

			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}

	}

	private byte[] readFileData(File file, int fileLength) throws IOException {
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

	// return supported MIME Types
	private String getContentType(String fileRequested) {
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

	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);

		httpResponse(out, dataOut, 404, content, fileLength, fileData);

		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}

	private void fileMoved(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_MOVED);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);

		httpResponse(out, dataOut, 301, content, fileLength, fileData);
		if (verbose) {
			System.out.println("File " + fileRequested + " moved permanently");
		}
	}

	/*
	 * Attenzione non stampa la prima riga dell'http response con il codice http (200 / 301 / 501 /404)
	 */
	 private void httpResponse(PrintWriter out, OutputStream dataOut, int httpCode, String contentMimeType, int fileLength, byte[] fileData) throws IOException {
		
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


	private root XmlDeserializer() throws IOException {
		// Deserializzo il file da XML a POJO
		File file = new File("src/main/resources/xml/classe.xml");
		XmlMapper xmlMapper = new XmlMapper();
		root value = xmlMapper.readValue(file, root.class);
		return value;
	}

	private String JsonSerializer(root value) throws IOException {
		// Serializzo da POJO a JSON
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(value);
	}

	private DeserializedJson JsonDeserializer() throws IOException {
		// Deserializzo il file da JSON a POJO

		File file = new File("src/main/resources/json/puntiVendita.json");
		ObjectMapper objectMapper = new ObjectMapper();
		DeserializedJson deserializedJson = objectMapper.readValue(file, DeserializedJson.class);
		return deserializedJson;
	}

	private String XmlSerializer(DeserializedJson value) throws IOException {
		// Serializzo da POJO a XML

		XmlMapper xmlMapper = new XmlMapper();
		xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
		return xmlMapper.writeValueAsString(value);
	}
}
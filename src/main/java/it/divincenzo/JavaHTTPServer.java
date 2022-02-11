package it.divincenzo;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

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
	private SerDesService serDesService = new SerDesService();
	private ServerService serverService = new ServerService();

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
				byte[] fileData = serverService.readFileData(file, fileLength);

				// we send HTTP Headers with data to client
				serverService.httpResponse(out, dataOut, 501, contentMimeType, fileLength, fileData);

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
					root deserializedXML = serDesService.XmlDeserializer();
					String s = serDesService.JsonSerializer(deserializedXML);
					fileLength = s.length();
					fileData = s.getBytes();
				} else if (fileRequested.endsWith(".xml")) {
					DeserializedJson deserializedJson = serDesService.JsonDeserializer();
					String s = serDesService.XmlSerializer(deserializedJson);
					fileLength = s.length();
					fileData = s.getBytes();
				} else {
					// file su file system (statico)
					// se il file non esiste provo a fare redrect
					File file = new File(WEB_ROOT, fileRequested);
					
					if (file.exists() && file.isFile()) {
						fileLength = (int) file.length();
						fileData = serverService.readFileData(file, fileLength);
					} else {
						serverService.fileMoved(out, dataOut, fileRequested, WEB_ROOT, FILE_MOVED);
						return;
					}
				}
				
				String content = serverService.getContentType(fileRequested);

				if (method.equals("GET")) { // GET method so we return content
					// send HTTP Headers
					serverService.httpResponse(out, dataOut, 200, content, fileLength, fileData);
				}

				if (verbose) {
					System.out.println("File " + fileRequested + " of type " + content + " returned");
				}

			}

		} catch (FileNotFoundException fnfe) {
			try {
				serverService.fileNotFound(out, dataOut, fileRequested, WEB_ROOT, FILE_NOT_FOUND);
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
}
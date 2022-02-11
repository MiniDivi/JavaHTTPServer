package it.divincenzo;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class SerDesService {
    protected root XmlDeserializer() throws IOException {
		// Deserializzo il file da XML a POJO
		File file = new File("src/main/resources/xml/classe.xml");
		XmlMapper xmlMapper = new XmlMapper();
		root value = xmlMapper.readValue(file, root.class);
		return value;
	}

	protected String JsonSerializer(root value) throws IOException {
		// Serializzo da POJO a JSON
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(value);
	}

	protected DeserializedJson JsonDeserializer() throws IOException {
		// Deserializzo il file da JSON a POJO

		File file = new File("src/main/resources/json/puntiVendita.json");
		ObjectMapper objectMapper = new ObjectMapper();
		DeserializedJson deserializedJson = objectMapper.readValue(file, DeserializedJson.class);
		return deserializedJson;
	}

	protected String XmlSerializer(DeserializedJson value) throws IOException {
		// Serializzo da POJO a XML

		XmlMapper xmlMapper = new XmlMapper();
		return xmlMapper.writeValueAsString(value);
	}
}

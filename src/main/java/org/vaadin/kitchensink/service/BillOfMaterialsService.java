package org.vaadin.kitchensink.service;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.springframework.stereotype.Service;

@Service
public class BillOfMaterialsService {

    public Bom getBom() {
        JsonParser parser = new JsonParser();
        try {
            URL bomFileUrl = BillOfMaterialsService.class.getResource("/resources/bom.json");
            Path bomFilePath = Paths.get(bomFileUrl.toURI());
            File bomFile = bomFilePath.toFile();
            return parser.parse(bomFile);
        } catch (URISyntaxException | ParseException e) {
            throw new BillOfMaterialsException("Failed to read bill of materials data", e);
        }
    }
}

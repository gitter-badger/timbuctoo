package nl.knaw.huygens.repository.importer.database;

import java.util.Date;

import nl.knaw.huygens.repository.config.BasicInjectionModule;
import nl.knaw.huygens.repository.config.Configuration;
import nl.knaw.huygens.repository.managers.StorageManager;
import nl.knaw.huygens.repository.model.dwcbia.DWCPlace;
import nl.knaw.huygens.repository.model.dwcbia.DWCScientist;
import nl.knaw.huygens.repository.model.raa.RAACivilServant;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class BulkImporter {

  public static void main(String[] args) throws Exception {
    Configuration config = new Configuration("config.xml");
    Injector injector = Guice.createInjector(new BasicInjectionModule(config));
    StorageManager storageManager = injector.getInstance(StorageManager.class);
    storageManager.getStorage().empty();

    GenericImporter importer = new GenericImporter();

    long beginTime = new Date().getTime();
    importer.importData("resources/DWCPlaceMapping.properties", storageManager, DWCPlace.class);
    importer.importData("resources/DWCScientistMapping.properties", storageManager, DWCScientist.class);
    importer.importData("resources/RAACivilServantMapping.properties", storageManager, RAACivilServant.class);
    CKCCPersonImporter csvImporter = new CKCCPersonImporter(storageManager);
    csvImporter.handleFile("testdata/ckcc-persons.txt", 9, false);
    long endTime = new Date().getTime();

    System.out.println("Import duration: " + ((endTime - beginTime) / 1000) + " seconds");

    storageManager.ensureIndices();
  }

}

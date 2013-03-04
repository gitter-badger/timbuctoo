package nl.knaw.huygens.repository.importer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import nl.knaw.huygens.repository.managers.StorageManager;
import nl.knaw.huygens.repository.model.Document;
import nl.knaw.huygens.repository.model.User;
import nl.knaw.huygens.repository.model.util.DocumentTypeRegister;
import nl.knaw.huygens.repository.pubsub.Hub;
import nl.knaw.huygens.repository.storage.Storage;
import nl.knaw.huygens.repository.storage.generic.StorageConfiguration;
import nl.knaw.huygens.repository.storage.generic.StorageFactory;
import nl.knaw.huygens.repository.util.Configuration;
import nl.knaw.huygens.repository.util.CryptoUtils;

public class SetupDatabase {

	private static final String FILE_FILTER = ".tab";
	private static File sourceDir;
	private static File jsonDir;
	private static BufferedWriter errors;
	private static Configuration conf = null;
  private static DbImporter importer;
  private static StorageManager storageManager;

	public static void main(String[] args) throws ConfigurationException, IOException {
	  initialize(new Configuration("config.xml"));
	  Hub hub = new Hub();
	  // FIXME: this should be configurable, and for that we need a commandline parsing tool.
	  String vreId = "test-vre";
	  String vreName = "Test VRE";

	  DocumentTypeRegister docTypeRegistry = new DocumentTypeRegister();
	  StorageConfiguration storageConfiguration = new StorageConfiguration(conf);
    Storage storage = StorageFactory.getInstance(storageConfiguration, docTypeRegistry);
    storageManager = new StorageManager(storageConfiguration, storage , hub, docTypeRegistry);
	  System.out.println("Emptying the database...");
	  storageManager.getStorage().empty();
	  System.out.println("Emptied the database.");

	  if (conf.getBooleanSetting("dataNeedsCleaning", false)) {
	    System.out.println("Cleaning input data...");
	    importCleaner();
	  }
	  importer = new DbImporter(conf, storageManager);
	  String[] models = conf.getSetting("doctypes").split(",");
	  for (String model : models) {
	    Class<? extends Document> cls = docTypeRegistry.getClassFromTypeString(model);
	    if (cls == null) {
	      System.err.println("Couldn't find a model for document type " + model + "! Are you sure you modeled everything correctly?");
	    } else {
	      importer.bulkImport(cls, true, vreId, vreName);
	    }
	  }
	  System.out.println("Creating indices...");
	  storageManager.ensureIndices();
	  System.out.println("Created indices.");
	  createAdminUser();
	  System.out.println("Done.");
	  System.exit(0);
	}

	private static void createAdminUser() throws IOException {
	  User admin = new User();
	  admin.setId(null); // Will be filled in by the storage implementation.
	  admin.email = "admin@example.com";
	  admin.pwHash = CryptoUtils.generatePwHash("password");
	  admin.groups = Lists.newArrayList("administrator");
	  admin.firstName = "Joe";
	  admin.lastName = "Administrator";
	  storageManager.addDocument(admin, User.class);
	  System.out.println("Added default user.");
  }

  protected static void initialize(Configuration conf) {
	  if (SetupDatabase.conf != null) {
	    return;
	  }
	  SetupDatabase.conf = conf;
		try {
			jsonDir = new File(conf.getSetting("paths.json", ""));
			jsonDir.delete();
			jsonDir.mkdir();

			File errorReport = new File("import-report.txt");
			errors = new BufferedWriter(new FileWriter(errorReport));

			sourceDir = new File(conf.getSetting("paths.source", ""));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void importCleaner() {
	  String charsetToUse = conf.getSetting("importencoding", "UTF8");
	  try {
	    System.out.println("Starting...");
	    FilenameFilter filter = new FilenameFilter() {
	      @Override
        public boolean accept(File dir, String name) {
	        return name.endsWith(FILE_FILTER);
	      }
	    };

	    ObjectMapper mapper = new ObjectMapper();

	    for (File importFile : sourceDir.listFiles(filter)) {
	      BufferedReader input =  new BufferedReader(new InputStreamReader(new FileInputStream(importFile), charsetToUse));
	      File outputFile = new File(jsonDir.getCanonicalPath() + '/' + importFile.getName().replace(FILE_FILTER, "") + ".json");
	      BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), charsetToUse));
	      String line = null;

	      errors.write("\n\n" + importFile.getName().replace(FILE_FILTER, "") + "\n");
	      errors.write("++++++++++++++++++++++++++++++++++++++++++++\n\n");
	      boolean noErrors = true;
	      int counter = 0;
	      while ((line = input.readLine()) != null) {
	        counter ++;
	        line = line.replace("\"{", "{");
	        line = line.replace("}\"", "}");
	        line = line.replace("\"\"", "\"");
	        line = line.replace("\"###null###\"", "null");
	        try {
	          Map<?,?> map = mapper.readValue(line, Map.class);
	          output.write(line + "\n");
	          System.out.print((counter % 10 == 9) ? map.get("^type") + " " + map.get("_id") + " |\n" : map.get("^type") + " " + map.get("_id") + " | ");
	        } catch (Exception e) {
	          noErrors = false;
	          System.out.print(e.getMessage() + " : " + line + "\n");
	          errors.write(e.getMessage() + " : " + line + "\n");
	        } finally {
	          output.flush();
	          errors.flush();
	        }
	      }

	      if (noErrors) {
	        errors.write("No Errors!");
	      }

	      System.out.println("\nPrepared and written " + importFile.getName() + "\n");
	    }
	  } catch (IOException e) {
	    throw new RuntimeException();
	  }
	}

}

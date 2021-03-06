package nl.knaw.huygens.timbuctoo.tools.importer;

/*
 * #%L
 * Timbuctoo tools
 * =======
 * Copyright (C) 2012 - 2015 Huygens ING
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import nl.knaw.huygens.timbuctoo.Repository;
import nl.knaw.huygens.timbuctoo.index.IndexManager;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.util.Change;
import nl.knaw.huygens.timbuctoo.tools.config.ToolsInjectionModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;

/**
 * This importer uses json-files created by the {@code BulkDataTransformer}, to import into a database.
 * This json-files are named like {internal class name}.json. The structure of the classes in these 
 * files should be the same as the structure that is communicated with client applications.
 */
public class TransformedDataImporter extends DefaultImporter {

  private static final Logger LOG = LoggerFactory.getLogger(TransformedDataImporter.class);

  public static void main(String[] args) throws Exception {
    String dataPath = args.length > 0 ? args[0] : "src/main/resources/testdata";

    Injector injector = ToolsInjectionModule.createInjector();
    Repository repository = injector.getInstance(Repository.class);
    IndexManager indexManager = injector.getInstance(IndexManager.class);

    TransformedDataImporter importer = new TransformedDataImporter(repository, indexManager);
    importer.importData(dataPath);
    importer.close();
  }

  public TransformedDataImporter(Repository repository, IndexManager indexManager) {
    super(repository, indexManager, "timbuctoo");
  }

  protected void importData(String dataPath) throws Exception {
    File[] jsonFiles = getJsonFiles(dataPath);

    for (File jsonFile : jsonFiles) {
      String className = jsonFile.getName().substring(0, jsonFile.getName().indexOf('.'));
      Class<? extends DomainEntity> type = repository.getTypeRegistry().getDomainEntityType(className);

      if (type != null) {
        removeNonPersistentEntities(type);
        save(type, jsonFile, change);
      } else {
        LOG.error("{} is not a DomainEntity", className);
      }
    }

    repository.close();
    indexManager.close();

  }

  protected File[] getJsonFiles(String dataPath) {
    File dataDir = new File(dataPath);
    File[] jsonFiles = dataDir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().endsWith(".json");
      }
    });
    return jsonFiles;
  }

  public <T extends DomainEntity> void save(Class<T> type, File jsonFile, Change change) throws Exception {
    LOG.info("Saving for type {}", type);
    List<T> entities = new ObjectMapper().readValue(jsonFile, new TypeReference<List<? extends DomainEntity>>() {});
    for (T entity : entities) {
      String id = repository.addDomainEntity(type, entity, change);
      indexManager.addEntity(type, id);
    }
  }

}

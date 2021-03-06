package nl.knaw.huygens.timbuctoo.tools.oaipmh.neww;

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

import com.google.inject.Injector;
import nl.knaw.huygens.timbuctoo.Repository;
import nl.knaw.huygens.timbuctoo.config.Configuration;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.neww.WWDocument;
import nl.knaw.huygens.timbuctoo.storage.StorageIterator;
import nl.knaw.huygens.timbuctoo.tools.oaipmh.OAIRecordCreator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.huygens.timbuctoo.tools.config.ToolsInjectionModule.createInjectorWithoutSolr;

public class WWOAIPMHTool {
  private static final String VRE_ID = "WomenWriters";
  private final static Logger LOG = LoggerFactory.getLogger(WWOAIPMHTool.class);
  private static String oaiUrl;

  public static void main(String args[]) throws Exception {
    Injector injector = createInjectorWithoutSolr();

    Configuration config = injector.getInstance(Configuration.class);
    oaiUrl = config.getSetting("oai-url");
    String frontEndURL = isProduction(oaiUrl) ? "http://resources.huygens.knaw.nl/womenwriters" : "http://www.example.com";

    OAIRecordCreator oaiRecordCreator = injector.getInstance(OAIRecordCreator.class);
    Repository repo = injector.getInstance(Repository.class);

    try {
      //      createOAIRecordForThreeEntities(WWDocument.class, frontEndURL, VRE_ID, oaiRecordCreator, repo);
      //      createOAIRecordForThreeEntities(WWPerson.class, frontEndURL, VRE_ID, oaiRecordCreator, repo);
      createOAIRecordForAllEntities(WWDocument.class, frontEndURL, VRE_ID, oaiRecordCreator, repo);
//      createOAIRecordForAllEntities(WWPerson.class, frontEndURL, VRE_ID, oaiRecordCreator, repo);
    } finally {
      repo.close();
    }
  }

  private static boolean isProduction(String oaiUrl) {

    return !StringUtils.contains(oaiUrl, "localhost:9998");
  }

  private static <T extends DomainEntity> void createOAIRecordForThreeEntities(Class<T> type, String frontEndURL, String vreId, OAIRecordCreator oaiRecordCreator, Repository repo) throws Exception {
    for (T entity : repo.getDomainEntities(type).getSome(3)) {
      repo.addRelationsToEntity(entity);
      createOAIRecord(frontEndURL, vreId, oaiRecordCreator, entity);
    }

  }

  private static <T extends DomainEntity> void createOAIRecordForAllEntities(Class<T> type, String frontEndURL, String vreId, OAIRecordCreator oaiRecordCreator, Repository repo) throws Exception {
    for (StorageIterator<T> iterator = repo.getDomainEntities(type); iterator.hasNext();) {
      T domainEntity = iterator.next();

      repo.addRelationsToEntity(domainEntity);

      createOAIRecord(frontEndURL, vreId, oaiRecordCreator, domainEntity);
    }
  }

  private static void createOAIRecord(String frontEndURL, String vreId, OAIRecordCreator oaiRecordCreator, DomainEntity entity) {
    LOG.info("create meta data for \"{}\" with id \"{}\"", entity.getClass(), entity.getId());
    oaiRecordCreator.create(entity, vreId, frontEndURL, oaiUrl);
  }
}

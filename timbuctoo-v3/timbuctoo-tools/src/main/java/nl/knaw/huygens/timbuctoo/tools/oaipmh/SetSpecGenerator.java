package nl.knaw.huygens.timbuctoo.tools.oaipmh;

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

import static nl.knaw.huygens.timbuctoo.tools.oaipmh.CollectionUtils.getSingularNameOfBaseCollection;
import static nl.knaw.huygens.timbuctoo.tools.oaipmh.VREIdUtils.simplifyVREId;

import java.util.List;
import java.util.Set;

import nl.knaw.huygens.oaipmh.MyOAISet;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.search.FilterableSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class SetSpecGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(SetSpecGenerator.class);
  private final OaiPmhRestClient oaiPmhClient;

  @Inject
  public SetSpecGenerator(OaiPmhRestClient oaiPmhClient) {
    this.oaiPmhClient = oaiPmhClient;
  }

  public List<String> generate(DomainEntity domainEntity, String vreId) {
    if (StringUtils.isBlank(vreId)) {
      throw new IllegalArgumentException("\"vreId\" should have a value");
    }

    if (domainEntity == null) {
      throw new IllegalArgumentException("\"domainEntity\" should have a value");
    }

    List<String> setSpecs = createSetSpec(domainEntity, vreId);

    createNewAOISets(setSpecs);

    return setSpecs;
  }

  private void createNewAOISets(List<String> setSpecs) {
    Set<String> filteredSetSpecs = filterSetSpecs(setSpecs);

    for (String setSpec : filteredSetSpecs) {
      LOG.info("creating new OAISet for setSpec \"{}\"", setSpec);
      oaiPmhClient.postSet(new MyOAISet().setSetSpec(setSpec).setName(setSpec.replace(":", " ")));
    }
  }

  private Set<String> filterSetSpecs(List<String> setSpecs) {
    FilterableSet<String> filterableSpecs = new FilterableSet<String>(Sets.newHashSet(setSpecs));

    List<MyOAISet> oaiSets = oaiPmhClient.getSets();

    return filterableSpecs.filter(new RemoveSpecsForExistingSetsPredicate(oaiSets));
  }

  private List<String> createSetSpec(DomainEntity domainEntity, String vreId) {
    String simplifyVREId = simplifyVREId(vreId);
    List<String> setSpec = Lists.newArrayList(simplifyVREId);

    addCollectionSpec(setSpec, domainEntity, simplifyVREId);

    return setSpec;
  }

  private void addCollectionSpec(List<String> setSpec, DomainEntity domainEntity, String simplifyVREId) {
    String baseCollection = getSingularNameOfBaseCollection(domainEntity);

    setSpec.add(String.format("%s:%s", simplifyVREId, baseCollection));
  }

  private static class RemoveSpecsForExistingSetsPredicate implements Predicate<String> {

    private List<MyOAISet> oaiSets;

    public RemoveSpecsForExistingSetsPredicate(List<MyOAISet> oaiSets) {
      this.oaiSets = oaiSets;
    }

    @Override
    public boolean apply(String input) {
      for (MyOAISet oaiSet : oaiSets) {
        if (Objects.equal(input, oaiSet.getSetSpec())) {
          return false;
        }
      }

      return true;
    }
  }

}

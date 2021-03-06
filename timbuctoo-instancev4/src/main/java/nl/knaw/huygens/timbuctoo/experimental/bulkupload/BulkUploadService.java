package nl.knaw.huygens.timbuctoo.experimental.bulkupload;

import nl.knaw.huygens.timbuctoo.experimental.bulkupload.parsedworkbook.ParsedWorkbook;
import nl.knaw.huygens.timbuctoo.experimental.bulkupload.parsedworkbook.RelationDescription;
import nl.knaw.huygens.timbuctoo.model.vre.Vre;
import nl.knaw.huygens.timbuctoo.model.vre.Vres;
import nl.knaw.huygens.timbuctoo.security.AuthorizationException;
import nl.knaw.huygens.timbuctoo.security.AuthorizationUnavailableException;
import nl.knaw.huygens.timbuctoo.security.Authorizer;
import nl.knaw.huygens.timbuctoo.server.GraphWrapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BulkUploadService {

  private final Vres vres;
  private final GraphWrapper graphwrapper;
  private final Authorizer authorizer;

  public BulkUploadService(Vres vres, GraphWrapper graphwrapper/*, Authorizer authorizer*/) {
    this.vres = vres;
    this.graphwrapper = graphwrapper;
    this.authorizer = null;//authorizer;
  }

  //FIXME: add authorizer on admin
  //FIXME: allow linking to existing vertices (e.g. geboorteplaats in emmigrantunits)

  public boolean saveToDb(String vreName, Workbook wb/*, String userId*/)
    throws AuthorizationUnavailableException, AuthorizationException {
    //
    //for (Collection collection : vre.getCollections().values()) {
    //  if (!authorizer.authorizationFor(collection, userId).isAllowedToWrite()) {
    //    throw new AuthorizationException(
    //      "You cannot use bulkupload because you are not allowed to edit " + collection.getCollectionName()
    //    );
    //  }
    //}

    ParsedWorkbook workbook = ParsedWorkbook.from(wb);
    Vre vre = vres.getVre(vreName);
    final Map<String, RelationDescription> descriptions = graphwrapper.getGraph().traversal()
      .V()
      .has("relationtype_regularName")
      .toList()
      .stream()
      .map(RelationDescription::new)
      .collect(
        HashMap::new,
        (map, desc) -> {
          map.put(desc.getRegularName(), desc);
          map.put(desc.getInverseName(), desc);
        },
        HashMap::putAll
      );

    dropAllVreVertices(vre);

    //FIXME: allow the excel sheet to specify more relationDescirptions
    if (workbook.saveToDb(graphwrapper, vre, descriptions)) {
      return true;
    }
    return false;
  }

  private void dropAllVreVertices(Vre vre) {
    final Set<String> keys = vre.getCollections().keySet();
    final String[] entityTypeNames = keys.toArray(new String[keys.size()]);
    P<String> labels = LabelP.of(entityTypeNames[0]);
    for (int i = 1; i < entityTypeNames.length; i++) {
      labels = labels.or(LabelP.of(entityTypeNames[i]));
    }
    try (Transaction tx = graphwrapper.getGraph().tx()) {
      graphwrapper.getGraph().traversal().V().has(T.label, labels).drop().toList();
      tx.commit();
    }
  }


  public Workbook getEmptyTemplate(String vreName, String... propsToLeaveOut) {
    Vre vre = vres.getVre(vreName);
    ParsedWorkbook workbook = new ParsedWorkbook();
    ////each property can generate the two rows needed for the excel
    ////furthermore all registered relations are generated
    //vre.getCollections().forEach((collName, coll) -> {
    //  if (coll.isRelationCollection()) {
    //    return;
    //  }
    //  CollectionRange sheet = CollectionRange.from(collName);
    //  List<Vertex> vertices = graphwrapper.getCurrentEntitiesFor(coll.getEntityTypeName()).toList();
    //  GraphTraversal<Vertex, Vertex> collectionTraversal = null;
    //  if (vertices.size() > 0) {
    //    collectionTraversal = graphwrapper.getGraph().traversal().V(vertices);
    //  }
    //
    //  for (Map.Entry<String, LocalProperty> entry : coll.getWriteableProperties().entrySet()) {
    //    LocalProperty prop = entry.getValue();
    //    PropertyColumns propertyColumns = sheet.withProperty(entry.getKey());
    //    if (collectionTraversal != null) {
    //      //propertyColumns.addData(collectionTraversal);//FIXME make it work
    //    }
    //  }
    //});
    return workbook.asWorkBook();
  }

}

package nl.knaw.huygens.repository.resources;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class SiteMapResourceTest extends WebServiceTestSetup {

  @Test
  public void testGetSitemap() {
    setUpUserRoles(Lists.newArrayList("USER"));
    WebResource resource = super.resource();

    ClientResponse response = resource.path("/api").header("Authorization", "bearer 12333322abef").get(ClientResponse.class);

    assertEquals(ClientResponse.Status.OK, response.getClientResponseStatus());
  }

  @Test
  public void testGetSitemapNotLoggedIn() {
    WebResource resource = super.resource();

    ClientResponse response = resource.path("/api").get(ClientResponse.class);

    assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getClientResponseStatus());
  }
}

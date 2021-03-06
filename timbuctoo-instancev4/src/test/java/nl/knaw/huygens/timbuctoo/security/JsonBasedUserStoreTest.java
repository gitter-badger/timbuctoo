package nl.knaw.huygens.timbuctoo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static nl.knaw.huygens.timbuctoo.util.OptionalPresentMatcher.present;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JsonBasedUserStoreTest {

  public static final Path USERS_FILE = Paths.get("src", "test", "resources", "users.json");
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private JsonBasedUserStore instance;

  @Before
  public void setUp() throws Exception {
    User user = new User();
    user.setPersistentId("pidOfKnownUser");
    User[] users = {user};
    OBJECT_MAPPER.writeValue(USERS_FILE.toFile(), users);

    instance = new JsonBasedUserStore(USERS_FILE);
  }

  @After
  public void tearDown() throws Exception {
    Files.delete(USERS_FILE);
  }

  @Test
  public void userForReturnsAnEmptyOptionalForAnUnknownUser() throws AuthenticationUnavailableException {
    Optional<User> unknownUser = instance.userFor("pidOfUnknownUser");

    assertThat(unknownUser, is(not(present())));
  }

  @Test
  public void userForReturnsAnOptionalWithAUserForAKnownUser() throws AuthenticationUnavailableException {
    Optional<User> knownUser = instance.userFor("pidOfKnownUser");

    assertThat(knownUser, is(present()));
  }

  @Test
  public void userForThrowsAnAuthenticationUnavailableExceptionWhenTheUsersFileCannotBeRead()
    throws AuthenticationUnavailableException {
    JsonBasedUserStore instance = new JsonBasedUserStore(Paths.get("nonExistingUserFile"));

    expectedException.expect(AuthenticationUnavailableException.class);

    instance.userFor("pid");
  }

  @Test
  public void createUserAddsAUserToTheUsersFile() throws Exception {
    String userId = instance.createUser("pidOfOtherUser", "email", "givenName", "surname", "organization");

    Optional<User> user = instance.userForId(userId);
    assertThat(user, is(present()));
    assertThat(user.get().getPersistentId(), is("pidOfOtherUser"));
  }

  @Test
  public void createUserLetsTheUserBeCreatedOnlyOnce() throws Exception {
    String userId1 = instance.createUser("pid", "email", "givenName", "surname", "organization");
    String userId2 = instance.createUser("pid", "email", "givenName", "surname", "organization");

    assertThat(userId1, is(userId2));

  }

  @Test
  public void createUserThrowsAUserCreationExceptionWhenTheUsersFileCannotBeRead() throws Exception {
    Path nonExistingUsersFile = Paths.get("src", "test", "resources", "users1.json");
    JsonBasedUserStore instance = new JsonBasedUserStore(nonExistingUsersFile);

    expectedException.expect(UserCreationException.class);
    instance.createUser("pid", "email", "givenName", "surname", "organization");

  }

}

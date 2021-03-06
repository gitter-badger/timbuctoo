package nl.knaw.huygens.timbuctoo.security;

import java.util.Optional;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserStoreMockBuilder {

  private final UserStore userStore;

  private UserStoreMockBuilder() throws AuthenticationUnavailableException {
    userStore = mock(JsonBasedUserStore.class);
    when(userStore.userFor(anyString())).thenReturn(Optional.empty());
  }

  public UserStore build() {
    return userStore;
  }

  public static UserStoreMockBuilder userStore() throws AuthenticationUnavailableException {
    return new UserStoreMockBuilder();
  }

  public UserStoreMockBuilder withUserFor(String pid) throws AuthenticationUnavailableException {
    when(userStore.userFor(pid)).thenReturn(Optional.of(new User()));
    return this;
  }

}

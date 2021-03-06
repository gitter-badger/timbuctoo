package nl.knaw.huygens.timbuctoo.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VreAuthorizationCollection {
  private final ObjectMapper objectMapper;
  private final Path authorizationsFolder;

  public VreAuthorizationCollection(Path authorizationsFolder) {
    objectMapper = new ObjectMapper();
    this.authorizationsFolder = authorizationsFolder;
  }

  public VreAuthorization addAuthorizationFor(String vreId, String userId, String userRole)
    throws AuthorizationUnavailableException {
    File file = getFile(vreId);
    VreAuthorization vreAuthorization = new VreAuthorization(vreId, userId, userRole);

    try {
      synchronized (authorizationsFolder) {
        List<VreAuthorization> authorizations = Lists.newArrayList();
        if (file.exists()) {
          authorizations =
            objectMapper.readValue(file, new TypeReference<List<VreAuthorization>>() {
            });
        }

        Optional<VreAuthorization> authOptional =
          authorizations.stream().filter(auth -> Objects.equals(auth.getUserId(), userId)).findFirst();
        if (authOptional.isPresent()) {
          return authOptional.get();
        }

        authorizations.add(vreAuthorization);
        synchronized (authorizationsFolder) {
          objectMapper.writeValue(file, authorizations.toArray(new VreAuthorization[authorizations.size()]));
        }
      }
    } catch (IOException e) {
      throw new AuthorizationUnavailableException(e.getMessage());
    }

    return vreAuthorization;
  }

  public Optional<VreAuthorization> authorizationFor(String vreId, String userId)
    throws AuthorizationUnavailableException {

    File file = getFile(vreId);

    if (!file.exists()) {
      return Optional.empty();
    }

    try {
      List<VreAuthorization> authorizations;
      synchronized (authorizationsFolder) {
        authorizations =
          objectMapper.readValue(file, new TypeReference<List<VreAuthorization>>() {
          });
      }
      return authorizations.stream()
                           .filter(authorization -> Objects.equals(authorization.getUserId(), userId))
                           .findAny();
    } catch (IOException e) {
      throw new AuthorizationUnavailableException(e.getMessage());
    }

  }

  private File getFile(String vreId) {
    return authorizationsFolder.resolve(String.format("%s.json", vreId)).toFile();
  }
}

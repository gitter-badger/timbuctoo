package nl.knaw.huygens.timbuctoo.persistence;

import java.io.IOException;

import javax.jms.JMSException;

import nl.knaw.huygens.persistence.PersistenceException;
import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.messages.Action;
import nl.knaw.huygens.timbuctoo.messages.Broker;
import nl.knaw.huygens.timbuctoo.messages.ConsumerService;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.storage.StorageManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class PersistenceService extends ConsumerService implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(PersistenceService.class);

  private final PersistenceWrapper persistenceWrapper;
  private final StorageManager storageManager;

  @Inject
  public PersistenceService(Broker broker, PersistenceWrapper persistenceWrapper, StorageManager storageManager) throws JMSException {
    super(broker, Broker.PERSIST_QUEUE, "PersistenceService");
    this.persistenceWrapper = persistenceWrapper;
    this.storageManager = storageManager;
  }

  @Override
  protected void executeAction(Action action) {

    switch (action.getActionType()) {
    case ADD:
    case MOD:
      setPID(action);
      break;
    case DEL:
      LOG.debug("Ignoring action {}", action);
      break;
    default:
      LOG.warn("Unexpected action {}", action);
      break;
    }

  }

  private void setPID(Action action) {
    Class<? extends Entity> type = action.getType();
    String pid = null;

    if (!TypeRegistry.isDomainEntity(type)) {
      LOG.error("Not a domain entitiy: {}", type.getSimpleName());
      return;
    }

    String id = action.getId();
    try {
      pid = persistenceWrapper.persistObject(type, id);
    } catch (PersistenceException ex) {
      LOG.error("Creating a PID for {} with id {} went wrong.", type, id);
      LOG.debug("Exception", ex);
    }

    try {
      storageManager.setPID(TypeRegistry.toDomainEntity(type), id, pid);
    } catch (IllegalStateException ex) {
      deletePID(pid);
      LOG.error("{} with id {} already has a PID", type, id);
    } catch (IOException ex) {
      LOG.error("Persisting {} with id {} went wrong", type, id);
      LOG.debug("Exception", ex);
    }
  }

  private void deletePID(String pid) {
    try {
      persistenceWrapper.deletePersistentId(pid);
    } catch (PersistenceException pe) {
      LOG.error("Deleting PID {} went wrong.", pid);
      LOG.debug("Exception", pe);
    }
  }

  @Override
  protected Logger getLogger() {
    return LOG;
  }

}

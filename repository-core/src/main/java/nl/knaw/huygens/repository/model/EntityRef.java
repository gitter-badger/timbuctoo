package nl.knaw.huygens.repository.model;

/**
 * A reference to an entity, to be used in other entities.
 * The reference is partially denormalized by including the display name.
 *
 * It is an open question whether we should include the variation.
 */
public class EntityRef {

  /**
   * Utility for creating instances.
   */
  public static <T extends Entity> EntityRef newInstance(String itype, String xtype, T entity) {
    return new EntityRef(itype, xtype, entity.getId(), entity.getDisplayName());
  }

  private String itype;
  private String xtype;
  private String id;
  private String displayName;

  // For deserialization...
  public EntityRef() {}

  public EntityRef(String itype, String xtype, String id, String displayName) {
    this.itype = itype;
    this.xtype = xtype;
    this.id = id;
    this.displayName = displayName;
  }

  public String getIType() {
    return itype;
  }

  public void setIType(String itype) {
    this.itype = itype;
  }

  public String getXType() {
    return xtype;
  }

  public void setXType(String xtype) {
    this.xtype = xtype;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof EntityRef) {
      EntityRef that = (EntityRef) object;
      return (this.itype == null ? that.itype == null : this.itype.equals(that.itype)) //
          && (this.xtype == null ? that.xtype == null : this.xtype.equals(that.xtype)) //
          && (this.id == null ? that.id == null : this.id.equals(that.id)) //
          && (this.displayName == null ? that.displayName == null : this.displayName.equals(that.displayName));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + (itype == null ? 0 : itype.hashCode());
    result = 31 * result + (id == null ? 0 : id.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return String.format("{%s,%s}", itype, id);
  }

}

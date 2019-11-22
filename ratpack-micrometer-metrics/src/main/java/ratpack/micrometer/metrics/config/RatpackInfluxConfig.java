package ratpack.micrometer.metrics.config;

public class RatpackInfluxConfig extends RatpackStepRegistryConfig<RatpackInfluxConfig> {
  /**
   * Tag that will be mapped to "host" when shipping metrics to Influx.
   */
  private String db = "mydb";

  /**
   * Write consistency for each point. Must be either one, any, quorum, or all (case insensitive).
   */
  private String consistency = "ONE";

  /**
   * Login user of the Influx server.
   */
  private String userName;

  /**
   * Login password of the Influx server.
   */
  private String password;

  /**
   * Retention policy to use (Influx writes to the DEFAULT retention policy if one is
   * not specified).
   */
  private String retentionPolicy;

  /**
   * Time period for which Influx should retain data in the current database. For
   * instance 7d, check the influx documentation for more details on the duration
   * format.
   */
  private String retentionDuration;

  /**
   * How many copies of the data are stored in the cluster. Must be 1 for a single node
   * instance.
   */
  private Integer retentionReplicationFactor;

  /**
   * Time range covered by a shard group. For instance 2w, check the influx
   * documentation for more details on the duration format.
   */
  private String retentionShardDuration;

  /**
   * URI of the Influx server.
   */
  private String uri = "http://localhost:8086";

  /**
   * Whether to enable GZIP compression of metrics batches published to Influx.
   */
  private boolean compressed = true;

  /**
   * Whether to create the Influx database if it does not exist before attempting to
   * publish metrics to it.
   */
  private boolean autoCreateDb = true;

  public String getDb() {
    return this.db;
  }

  public RatpackInfluxConfig db(String db) {
    this.db = db;
    return this;
  }

  public String getConsistency() {
    return this.consistency;
  }

  public RatpackInfluxConfig consistency(String consistency) {
    this.consistency = consistency;
    return this;
  }

  public String getUserName() {
    return this.userName;
  }

  public RatpackInfluxConfig userName(String userName) {
    this.userName = userName;
    return this;
  }

  public String getPassword() {
    return this.password;
  }

  public RatpackInfluxConfig password(String password) {
    this.password = password;
    return this;
  }

  public String getRetentionPolicy() {
    return this.retentionPolicy;
  }

  public RatpackInfluxConfig retentionPolicy(String retentionPolicy) {
    this.retentionPolicy = retentionPolicy;
    return this;
  }

  public String getRetentionDuration() {
    return this.retentionDuration;
  }

  public RatpackInfluxConfig retentionDuration(String retentionDuration) {
    this.retentionDuration = retentionDuration;
    return this;
  }

  public Integer getRetentionReplicationFactor() {
    return this.retentionReplicationFactor;
  }

  public RatpackInfluxConfig retentionReplicationFactor(Integer retentionReplicationFactor) {
    this.retentionReplicationFactor = retentionReplicationFactor;
    return this;
  }

  public String getRetentionShardDuration() {
    return this.retentionShardDuration;
  }

  public RatpackInfluxConfig retentionShardDuration(String retentionShardDuration) {
    this.retentionShardDuration = retentionShardDuration;
    return this;
  }

  public String getUri() {
    return this.uri;
  }

  public RatpackInfluxConfig uri(String uri) {
    this.uri = uri;
    return this;
  }

  public boolean isCompressed() {
    return this.compressed;
  }

  public RatpackInfluxConfig compressed(boolean compressed) {
    this.compressed = compressed;
    return this;
  }

  public boolean isAutoCreateDb() {
    return this.autoCreateDb;
  }

  public RatpackInfluxConfig autoCreateDb(boolean autoCreateDb) {
    this.autoCreateDb = autoCreateDb;
    return this;
  }
}

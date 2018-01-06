package ratpack.dropwizard.metrics;

public class ByteBufAllocatorConfig {
  private boolean enabled = true;
  private boolean detailed = true;

  /* The flag whether byte buf allocator metric set should be initialized.
   * @return the flag
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set the flag whether byte buf allocator metric set should be initialized.
   *
   * @param enabled True if metrics set should be initialzed. False otherwise
   * @return this
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /* The flag whether byte buf allocator metric set should be report detailed metrics.
   * @return the flag
   */
  public boolean isDetailed() {
    return detailed;
  }

  /**
   * Set the flag whether byte buf allocator metric set should be report detailed metrics.
   *
   * @param detailed True if metrics set should be report detailed metrics. False otherwise
   * @return this
   */
  public void setDetailed(boolean detailed) {
    this.detailed = detailed;
  }
}

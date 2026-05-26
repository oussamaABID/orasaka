package com.orasaka.gateway.infrastructure.adapter.amqp;

/** Checked exception thrown when a job execution strategy fails to process a job message. */
public class JobExecutionException extends Exception {

  private static final long serialVersionUID = 1L;

  public JobExecutionException(String message) {
    super(message);
  }

  public JobExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}

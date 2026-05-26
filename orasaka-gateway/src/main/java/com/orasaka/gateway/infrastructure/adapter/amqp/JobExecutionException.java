package com.orasaka.gateway.infrastructure.adapter.amqp;

/**
 * Checked exception thrown when a job execution strategy fails to process a job message.
 *
 */
public class JobExecutionException extends Exception {

  public JobExecutionException(String message) {
    super(message);
  }

  public JobExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}

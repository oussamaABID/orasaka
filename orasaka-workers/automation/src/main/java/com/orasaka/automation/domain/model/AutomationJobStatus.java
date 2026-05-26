package com.orasaka.automation.domain.model;

/**
 * Enumeration representing the lifecycle states of an automation job.
 *
 * <p>Jobs follow a strict state machine flow:
 *
 * <pre>
 *   PENDING_APPROVAL → APPROVED → RUNNING → COMPLETED
 *                                         → FAILED
 *                    → AWAITING_CLI_EXECUTION → RUNNING → COMPLETED
 * </pre>
 *
 * @since 2.0.0
 */
public enum AutomationJobStatus {

  /** Job created but awaiting user approval before execution. */
  PENDING_APPROVAL,

  /** Job approved by an authorized user — ready for dispatch. */
  APPROVED,

  /** Job is currently being executed by the worker or CLI agent. */
  RUNNING,

  /** Job completed successfully. */
  COMPLETED,

  /** Job execution failed with an error. */
  FAILED,

  /** Job requires bare-metal CLI execution — waiting for agent connection. */
  AWAITING_CLI_EXECUTION
}

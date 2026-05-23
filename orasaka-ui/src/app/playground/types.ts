export interface TargetExecutionUri {
  uriPath: string;
  httpMethod: string;
  payloadTemplate?: string;
}

export interface NodeState {
  type: "ACTIVE" | "LOCKED" | "INVISIBLE";
  reason?: string;
  lockedAt?: string;
}

export interface OperationNode {
  id: string;
  label: string;
  icon: string;
  presentationContext: string;
  state: NodeState;
  executionDetails: TargetExecutionUri;
}

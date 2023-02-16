export interface MatterPlugin {
  configure(options: {
    deviceControllerKey: string;
    caRootCert: string;
    fabricId: string;
    vendorId: number;
  }): Promise<void>;
  clear(): Promise<void>;
  startCommissioning(options: {
    deviceId: string;
  }): Promise<{ deviceType: string }>;

  commandOnOff(options: {
    deviceId: string;
    value: boolean;
    endpointId: number;
  }): Promise<void>;

  // stateOnOff(options: { deviceId: string }): Promise<{ state: boolean }>;

  getEndpoint<T>(options: {
    deviceId: string;
    endpointId: number;
  }): Promise<{ data: T }>;

  getCluster<T>(options: {
    deviceId: string;
    endpointId: number;
    clusterId: number;
  }): Promise<{ data: T }>;

  getAttribute<T>(options: {
    deviceId: string;
    endpointId: number;
    clusterId: number;
    attributeId: number;
  }): Promise<{ data: T }>;
}

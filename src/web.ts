import { WebPlugin } from '@capacitor/core';

import type { MatterPlugin } from './definitions';

export class MatterWeb extends WebPlugin implements MatterPlugin {
  configure(options: {
    deviceControllerKey: string;
    caRootCert: string;
    fabricId: string;
    vendorId: number;
  }): Promise<void> {
    console.log(options);
    throw new Error('Method not implemented.');
  }
  clear(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  startCommissioning(options: {
    deviceId: string;
  }): Promise<{ deviceType: string }> {
    console.log(options);
    throw new Error('Method not implemented.');
  }
  commandOnOff(options: { value: boolean; deviceId: string; endpointId: number; }): Promise<void> {
    console.log(options);
    throw new Error('Method not implemented.');
  }

  getEndpoint<T>(options: {
    deviceId: string;
    endpointId: number;
  }): Promise<{ data: T }> {
    console.log(options);
    throw new Error('Method not implemented.');
  }
  getCluster<T>(options: {
    deviceId: string;
    endpointId: number;
    clusterId: number;
  }): Promise<{ data: T }> {
    console.log(options);
    throw new Error('Method not implemented.');
  }
  getAttribute<T>(options: {
    deviceId: string;
    endpointId: number;
    clusterId: number;
    attributeId: number;
  }): Promise<{ data: T }> {
    console.log(options);
    throw new Error('Method not implemented.');
  }
}

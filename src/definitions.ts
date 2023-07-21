import type { PluginListenerHandle } from '@capacitor/core';

export interface MatterPlugin {
  configure(options: {
    deviceControllerKey?: string;
    caRootCert?: string;
    fabricId: string;
    vendorId: number;
  }): Promise<void>;
  clear(): Promise<void>;
  startCommissioning(options: {
    deviceId: string;
  }): Promise<{ deviceType: string }>;

  getCerts(): Promise<{ deviceControllerKey: string; caRootCert: string }>;

  commandOnOff(options: {
    deviceId: string;
    value: boolean;
    endpointId: number;
  }): Promise<void>;

  getEndpoint<T>(options: {
    deviceId: string;
    endpointId: number;
  }): Promise<{ data: T }>;

  getCluster<T>(options: {
    deviceId: string;
    endpointId: number;
    clusterId: number;
  }): Promise<{ data: T }>;

  readAttribute(options: AttributePath): Promise<{ value: string }>;

  subscribeAttribute(options: SubscriberOptions): Promise<void>;

  addListener<T>(
    eventName: string,
    listenerFunc: AttributeChangeListener<T>,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}

export interface AttributePath {
  deviceId: string;
  endpointId: number;
  clusterId: number;
  attributeId: number;
}

export interface EventOptions {
  eventName: string;
  min: number;
  max: number;
}

export type SubscriberOptions = EventOptions & AttributePath;

export declare type AttributeChangeListener<T> = (data: T) => void;

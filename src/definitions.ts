import type { PluginListenerHandle } from '@capacitor/core';

export interface MatterPlugin {
  configure(options: {
    deviceControllerKey?: string;
    caRootCert?: string;
    fabricId: string;
    vendorId: number;
  }): Promise<void>;
  clear(): Promise<void>;

  qrCodeCommissioning(options: {
    deviceId: string;
    qrCodeId: string;
    ssid: string;
    ssidPassword: string;
  }): Promise<{ deviceType: string }>;

  manualCodeCommissioning(options: {
    deviceId: string;
    manualCode: string;
    ssid: string;
    ssidPassword: string;
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

  openCommissioningWindow(options: {
    deviceId: string;
    discriminator: number;
    duration: number;
  }): Promise<{ manualCode: string }>;

  removeFabric(options: { deviceId: string; fabricId: string }): Promise<void>;

  removeAllFabric(options: { deviceId: string }): Promise<void>;

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

export enum ErrorStatus {
  Close = '-1',
  GenericError = '-2',
  RendezVousError = '-3',
  StartCommissioningError = '-4',
  ParseQrCodeError = '-5',
  GetDeviceConnectedError = '-6',
  PairDeviceError = '-7',
  CommissioningEndError = '-8',
  BluetoothDisabled = '-9',
  ParseManualCodeError = '-10',
  getDeviceError = '-11',
  openCommissioningWindowError = '-12',
}

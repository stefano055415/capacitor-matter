import { WebPlugin } from '@capacitor/core';
import type { AttributePath, MatterPlugin, SubscriberOptions } from './definitions';
export declare class MatterWeb extends WebPlugin implements MatterPlugin {
    removeFabric(options: {
        deviceId: string;
        fabricId: string;
    }): Promise<void>;
    removeAllFabric(options: {
        deviceId: string;
    }): Promise<void>;
    openCommissioningWindow(options: {
        deviceId: string;
        discriminator: number;
        duration: number;
    }): Promise<{
        manualCode: string;
    }>;
    qrCodeCommissioning(options: {
        deviceId: string;
        qrCodeId: string;
        ssid: string;
        ssidPassword: string;
    }): Promise<{
        deviceType: string;
    }>;
    manualCodeCommissioning(options: {
        deviceId: string;
        manualCode: string;
        ssid: string;
        ssidPassword: string;
    }): Promise<{
        deviceType: string;
    }>;
    getCerts(): Promise<{
        deviceControllerKey: string;
        caRootCert: string;
    }>;
    readAttribute(options: AttributePath): Promise<{
        value: string;
    }>;
    subscribeAttribute(options: SubscriberOptions): Promise<void>;
    configure(options: {
        deviceControllerKey?: string;
        caRootCer?: string;
        fabricId: string;
        vendorId: number;
    }): Promise<void>;
    clear(): Promise<void>;
    commandOnOff(options: {
        value: boolean;
        deviceId: string;
        endpointId: number;
    }): Promise<void>;
    getEndpoint<T>(options: {
        deviceId: string;
        endpointId: number;
    }): Promise<{
        data: T;
    }>;
    getCluster<T>(options: {
        deviceId: string;
        endpointId: number;
        clusterId: number;
    }): Promise<{
        data: T;
    }>;
    getAttribute<T>(options: {
        deviceId: string;
        endpointId: number;
        clusterId: number;
        attributeId: number;
    }): Promise<{
        data: T;
    }>;
}

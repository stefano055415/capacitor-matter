//
//  RequestHandler.swift
//  MatterExtension
//
//  Created by Vittorio Lo Preiato on 18/07/23.
//

import MatterSupport

// The extension is launched in response to `MatterAddDeviceRequest.perform()` and this class is the entry point
// for the extension operations.
@available(iOS 16.4, *)
public class MatterRequestHandler {
    public init() {}
    
    public func validateDeviceCredential(_ deviceCredential: MatterAddDeviceExtensionRequestHandler.DeviceCredential) async throws {
        print("matter")
        // Use this function to perform additional attestation checks if that is useful for your ecosystem.
    }

    public func selectWiFiNetwork(_ wifiScanResults: [MatterAddDeviceExtensionRequestHandler.WiFiScanResult]) async throws -> MatterAddDeviceExtensionRequestHandler.WiFiNetworkAssociation {
        // Use this function to select a Wi-Fi network for the device if your ecosystem has special requirements.
        // Or, return `.defaultSystemNetwork` to use the iOS device's current network.
        print("matter")
        return .defaultSystemNetwork
    }

    public func selectThreadNetwork(_ threadScanResults: [MatterAddDeviceExtensionRequestHandler.ThreadScanResult]) async throws -> MatterAddDeviceExtensionRequestHandler.ThreadNetworkAssociation {
        // Use this function to select a Thread network for the device if your ecosystem has special requirements.
        // Or, return `.defaultSystemNetwork` to use the default Thread network.
        print("matter")
        return .defaultSystemNetwork
    }

    public func commissionDevice(home: MatterAddDeviceRequest.Home?,onboardingPayload: String, commissioningID: UUID) async throws {
        // Use this function to commission the device with your Matter stack.
        let controller = InitializeMTR()
        let deviceId = MTRGetLastPairedDeviceId()
        
        do{
            try controller?.pairDevice(deviceId + 1, onboardingPayload: onboardingPayload)
        } catch {
            print("matter")
        }
        
//        do{
//            
//            let  device = try controller?.deviceBeingCommissioned(withNodeID: 1);
//        }catch {
//            print("matter")
//        }
//
//        if (device?.sessionTransportType == MTRTransportType.BLE) {
//            print("ble")
////            dispatch_async(dispatch_get_main_queue(), ^{
////                [self->_deviceList refreshDeviceList];
////                [self retrieveAndSendWiFiCredentials];
////            });
//        }
//        else {
//            MTRCommissioningParameters * params = [[MTRCommissioningParameters alloc] init];
//            params.deviceAttestationDelegate = [[CHIPToolDeviceAttestationDelegate alloc] initWithViewController:self];
//            params.failSafeExpiryTimeoutSecs = @600;
//            NSError * error;
//            if (![controller commissionDevice:deviceId commissioningParams:params error:&error]) {
//                NSLog(@"Failed to commission Device %llu, with error %@", deviceId, error);
//            }
//        }
        
        
        
        
        
        
        
//        let params = MTRCommissioningParameters()
////        params.deviceAttestationDelegate = CHIPToolDeviceAttestationDelegate.initWithViewController();
//        params.failSafeExpiryTimeoutSecs = 600;
//       do {
//           let result = try controller?.commissionNode(withID: 1, commissioningParams: params)
//       } catch {
//           print("matter")
//       }
        print("matter")
    }

    public func rooms(_ home: MatterAddDeviceRequest.Home?) async -> [MatterAddDeviceRequest.Room] {
        // Use this function to return the rooms your ecosystem manages.
        // If your ecosystem manages multiple homes, ensure you are returning rooms that belong to the provided home.
        print("matter")
        return [.init(displayName: "Living Room")]
    }

    public func configureDevice(name: String, room: MatterAddDeviceRequest.Room?) async {
        print("matter")
        // Use this function to configure the (now) commissioned device with the given name and room.
    }
}



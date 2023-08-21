import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@available(iOS 16.4, *)
@objc(MatterPlugin)
public class MatterPlugin: CAPPlugin {
    private let implementation = Matter()
    
    
    @objc func configure(_ call: CAPPluginCall) {
        let deviceControllerKey = call.getString("deviceControllerKey")
        let caRootCert = call.getString("caRootCert")
        let fabricStringId = call.getString("fabricId")
        let vendorId = call.getInt("vendorId")
        
        
        
        if (fabricStringId == nil || vendorId == nil) {
          call.reject("params must be exist!")
          return;
        }

//        do {
          let fabricId =  Int(fabricStringId!)
            implementation.configure(deviceControllerKey: deviceControllerKey, caRootCert: caRootCert, fabricId: fabricId!, vendorId: Int32(vendorId!), call: call)
//        } catch {
//          call.reject("fabricId must be a number and not major of 9223372036854775807")
//        }
        
    }
    
    @objc func readAttribute(_ call: CAPPluginCall) {
    
        let deviceStringId = call.getString("deviceId")
        let endpointId = call.getInt("endpointId")
        let clusterId = call.getInt("clusterId")
        let attributeId = call.getInt("attributeId")

        if (deviceStringId == nil || endpointId == nil || clusterId == nil || attributeId == nil) {
          call.reject("params must be exist!")
          return;
        }
        
        let deviceId =  UInt64(deviceStringId!)
        implementation.readAttribute(deviceId: deviceId!, endpointId: endpointId!, clusterId: clusterId!, attributeId: attributeId!, call: call)

    }


    @objc func qrCodeCommissioning(_ call: CAPPluginCall) {
        let qrCodeId = call.getString("qrCodeId")
        let deviceId = call.getString("deviceId")
        let ssid = call.getString("ssid")
        let ssidPassword = call.getString("ssidPassword")
        
        if (qrCodeId == nil || deviceId == nil || ssid == nil || ssidPassword == nil) {
          call.reject("params must be exist!")
          return;
        }
        
        implementation.qrCodeCommissioning(qrCodeId: qrCodeId!, deviceId: UInt64(deviceId!) ?? 1, ssid: ssid!, ssidPassword: ssidPassword!, call: call)
    }

    @objc func manualCodeCommissioning(_ call: CAPPluginCall) {
        let manualCode = call.getString("manualCode")
        let deviceId = call.getString("deviceId")
        let ssid = call.getString("ssid")
        let ssidPassword = call.getString("ssidPassword")
        
        if (manualCode == nil || deviceId == nil || ssid == nil || ssidPassword == nil) {
          call.reject("params must be exist!")
          return;
        }
        
        implementation.manualCodeCommissioning(manualCode: manualCode!, deviceId: UInt64(deviceId!) ?? 1, ssid: ssid!, ssidPassword: ssidPassword!, call: call)
        
    }
    
}

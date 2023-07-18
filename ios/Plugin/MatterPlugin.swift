import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(MatterPlugin)
public class MatterPlugin: CAPPlugin {
    private let implementation = Matter()

    @objc func startCommissioning(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": implementation.startCommissioning("data")
        ])
        
    }
    
}

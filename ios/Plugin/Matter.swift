import Foundation
import MatterSupport

@objc public class Matter: NSObject {
    @objc public func startCommissioning(_ value: String)  -> String {
        
        if #available(iOS 16.1, *) {
            let homes = [MatterAddDeviceRequest.Home(displayName: "My Home")]
            let topology = MatterAddDeviceRequest.Topology(ecosystemName: "MyEcosystemName", homes: homes)
            
            
            let request = MatterAddDeviceRequest(topology: topology)
            
//            let onboardingPayload = "MT:U1ZA3IAN165DHG3Z800"
//            let qrCodeParser = MTRQRCodeSetupPayloadParser(base38Representation: onboardingPayload)
//            request.setupPayload = qrCodeParser.populatePayload()
            
            Task {
                do {
                   let data = try  await request.perform()
                    print("Successfully set up device!")
                } catch {
                    print("Failed to set up device with error: \(error)")
                }
            }
            
        } else {
            // or use some work around
        }
        
        
        print(value)
        return value
    }
}

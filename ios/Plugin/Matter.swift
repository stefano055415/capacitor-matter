import Foundation
import Capacitor

@available(iOS 16.4, *)
public class Matter: MTRDeviceControllerDelegate  {
    var _fabricId: Int = 1;
    var _vendorId: Int32 = 0xFFF4;
    var _deviceId: UInt64 = 1;
    var _ssid: String = "";
    var _ssidPassword: String = "";
    var _call: CAPPluginCall? = nil;
    
    public func isEqual(_ object: Any?) -> Bool {
        return false;
    }
    
    public var hash: Int = 0
    
    public var superclass: AnyClass?
    
    public func `self`() -> Self {
        return self
    }
    
    public func perform(_ aSelector: Selector!) -> Unmanaged<AnyObject>! {
        return nil
    }
    
    public func perform(_ aSelector: Selector!, with object: Any!) -> Unmanaged<AnyObject>! {
        return nil
    }
    
    public func perform(_ aSelector: Selector!, with object1: Any!, with object2: Any!) -> Unmanaged<AnyObject>! {
        return nil
    }
    
    public func isProxy() -> Bool {
        return false
    }
    
    public func isKind(of aClass: AnyClass) -> Bool {
        return false
    }
    
    public func isMember(of aClass: AnyClass) -> Bool {
        return false
    }
    
    public func conforms(to aProtocol: Protocol) -> Bool {
        return false
    }
    
    public func responds(to aSelector: Selector!) -> Bool {
        return true;
    }
    
    public var description: String = ""
    
    public func controller(_ controller: MTRDeviceController, statusUpdate status: MTRCommissioningStatus) {
        return;
    }
    
    public func controller(_ controller: MTRDeviceController, readCommissioningInfo info: MTRProductIdentity) {
        return;
    }
    
    public func controller(_ controller: MTRDeviceController, commissioningComplete error: Error?, nodeID: NSNumber?) {
        _call?.resolve()
        _call = nil
        return
    }
    
    public func controller(_ controller: MTRDeviceController, commissioningComplete error: Error?) {
        _call?.resolve()
        _call = nil
        return;
    }
    
    public func controller(_ controller: MTRDeviceController, commissioningSessionEstablishmentDone error: Error?) {
        print("sono qui")
        
        if (error != nil) {
            print("Got pairing error back");
        } else {
//            let deviceId = MTRGetLastPairedDeviceId();
            do{
                let device: MTRBaseDevice = try controller.deviceBeingCommissioned(withNodeID: _deviceId as NSNumber);
                if (device.sessionTransportType == MTRTransportType.BLE) {
                    self.commissionWithSSID()                  
                } else {
                    let params: MTRCommissioningParameters = MTRCommissioningParameters.init();
//                    params.deviceAttestationDelegate = CHIPToolDeviceAttestationDelegate.init(viewController: self);

                    params.failSafeExpiryTimeoutSecs = 600;

                    try _controller?.commissionDevice(_deviceId, commissioningParams: params)
                }
            } catch{
                print("Failed to commission Device" + String(_deviceId) + ", with error ")
            }

        }
    }
    
    var _controller: MTRDeviceController? = nil;
    var _setupPayload: MTRSetupPayload? = nil;
    
    @objc public func configure(deviceControllerKey: String?,
                                 caRootCert: String?,
                                 fabricId: Int,
                                 vendorId: Int32,
                                 call: CAPPluginCall)  -> Void
    {
        _fabricId = fabricId;
        _vendorId = vendorId;
        _controller = InitializeMTR(_fabricId, _vendorId)
        call.resolve()
    }
    
    @objc public func startCommissioning(call: CAPPluginCall )  -> Void {
        
    }
    @objc public func manualCommissioning(qrCodeId: String, deviceId: UInt64, ssid: String, ssidPassword: String, call: CAPPluginCall )  -> Void {
        _deviceId = deviceId;
        _ssid = ssid;
        _ssidPassword = ssidPassword
        _call = call;
        let parser = MTRQRCodeSetupPayloadParser.init(base38Representation: qrCodeId)
        do{
            _setupPayload = try parser.populatePayload()
            handleRendezVous(deviceId: deviceId, setupPayload: _setupPayload!, rawPayload: qrCodeId)
        } catch {
            
        }
        print("manual commissioning")
    }
    
    @objc public func readAttribute(deviceId: UInt64, endpointId: Int, clusterId: Int, attributeId: Int, call: CAPPluginCall )  -> Void {
        _deviceId = deviceId;
        _call = call;
        MTRGetConnectedDeviceWithID(deviceId){ chipDevice,error in
            if(error != nil){
                call.reject("-2")
                return;
            }
            let serialQueue = DispatchQueue(label: "com.csa.matter.qrcodevc.callback")           
            let attribute = MTRAttributeRequestPath.init(endpointID: endpointId as NSNumber, clusterID: clusterId as NSNumber, attributeID: attributeId as NSNumber)
            
            chipDevice?.readAttributePaths([attribute], eventPaths: nil, params: nil, queue: serialQueue) {result,error in 
                for (key, values) in result![0] {
                    if let myDictionary = values as? [String : String] {
                        if(myDictionary["value"] != nil) {
                            call.resolve(["value": myDictionary["value"]!])
                        }
                    }
                }
            }
        }
    }
    
    
    private func handleRendezVous(deviceId: UInt64, setupPayload: MTRSetupPayload, rawPayload: String)
    {
        if (setupPayload.rendezvousInformation == nil) {
            handleRendezVousDefault(deviceId: deviceId, payload: rawPayload)
            return;
        }

        switch(setupPayload.discoveryCapabilities) {
            case MTRDiscoveryCapabilities.softAP:
                print("Rendezvous Wi-Fi");
                break;
            default:
                print("Rendezvous Default");
                handleRendezVousDefault(deviceId: deviceId, payload: rawPayload)
        }
    }
    
    private func handleRendezVousDefault(deviceId: UInt64, payload: String)
    {
        let deviceID = MTRGetNextAvailableDeviceID();

        restartMatterStack()
        do {
            let result = try _controller!.pairDevice(deviceId, onboardingPayload: payload)
        } catch {
            
        }
        
    }
    
    
    private func restartMatterStack ()
    {
        _controller = MTRRestartController(_controller!);
        let serialQueue = DispatchQueue(label: "com.csa.matter.qrcodevc.callback")
        _controller!.setDeviceControllerDelegate(self, queue: serialQueue)
    }
    
    private func commissionWithSSID()
    {
        Task{
            await Task.sleep(3)
            _controller = InitializeMTR(_fabricId, _vendorId)
             // create commissioning params in ObjC. Pass those in here with network credentials.
             // maybe this just becomes the new norm
             let params = MTRCommissioningParameters.init();
             params.wifiSSID = _ssid.data(using: .utf8)
             params.wifiCredentials = _ssidPassword.data(using: .utf8)
             params.failSafeExpiryTimeoutSecs = 600;
             do {
                 try _controller?.commissionDevice(_deviceId, commissioningParams: params)
             } catch {
                 print("Failed to commission Device" + String(_deviceId) + ", with error ")
             }
        }
        
       
    
    }
    
}

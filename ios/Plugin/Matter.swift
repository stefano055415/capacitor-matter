import Foundation
import Capacitor

public class MatterAttestationDelegate: MTRDeviceAttestationDelegate{
    public func isEqual(_ object: Any?) -> Bool {
        return true
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
        return true
    }
    
    public func isKind(of aClass: AnyClass) -> Bool {
        return true
    }
    
    public func isMember(of aClass: AnyClass) -> Bool {
        return true
    }
    
    public func conforms(to aProtocol: Protocol) -> Bool {
        return true
    }
    
    public func responds(to aSelector: Selector!) -> Bool {
        return true
    }
    
    public var description: String = ""
    
    
    public func deviceAttestationFailed(for controller: MTRDeviceController, opaqueDeviceHandle: UnsafeMutableRawPointer, error: Error) {
        return;
    }
    
    public func deviceAttestationCompleted(for controller: MTRDeviceController, opaqueDeviceHandle: UnsafeMutableRawPointer, attestationDeviceInfo: MTRDeviceAttestationDeviceInfo, error: Error?) {
        do {
         try controller.continueCommissioningDevice(opaqueDeviceHandle, ignoreAttestationFailure: true)
        } catch {
            print("error on ignore opaqueDevice")
        }
        return;
    }
    
}

@available(iOS 16.4, *)
public class Matter: MTRDeviceControllerDelegate  {
    var _fabricId: Int = 1;
    var _vendorId: Int32 = 0xFFF4;
    var _deviceId: UInt64 = 1;
    var _ssid: String = "";
    var _ssidPassword: String = "";
    var _call: CAPPluginCall? = nil;
    var _controller: MTRDeviceController? = nil;
    var _setupPayload: MTRSetupPayload? = nil;
    
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
        if(status == MTRCommissioningStatus.failed){
            _call?.reject("-3")
            _call = nil
            return;
        }
    }
    
    public func controller(_ controller: MTRDeviceController, readCommissioningInfo info: MTRProductIdentity) {
        return;
    }
    
    public func controller(_ controller: MTRDeviceController, commissioningComplete error: Error?, nodeID: NSNumber?) {
        if(error != nil){
            _call?.reject("-8")
            _call = nil
            return;
        }
        _call?.resolve()
        _call = nil
        return
    }
    
    public func controller(_ controller: MTRDeviceController, commissioningComplete error: Error?) {
        if(error != nil){
            _call?.reject("-8")
            _call = nil
            return;
        }
        _call?.resolve()
        _call = nil
        return;
    }
    
    public func controller(_ controller: MTRDeviceController, commissioningSessionEstablishmentDone error: Error?) {
        if (error != nil) {
            _call?.reject("-3")
            _call = nil
            return;
        }
        do {
            let device: MTRBaseDevice = try controller.deviceBeingCommissioned(withNodeID: _deviceId as NSNumber);
            
            if (device.sessionTransportType == MTRTransportType.BLE) {
                self.commissionWithSSID()
            } else {
                let params: MTRCommissioningParameters = MTRCommissioningParameters.init();
                params.deviceAttestationDelegate = MatterAttestationDelegate.init()
                params.failSafeExpiryTimeoutSecs = 600;

                try _controller?.commissionDevice(_deviceId, commissioningParams: params)
            }
        } catch {
            print("Failed to commission Device" + String(_deviceId) + ", with error ")
            _call?.reject("-4")
            _call = nil
            return;
        }

        
    }
    

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
        
        let parser = MTRQRCodeSetupPayloadParser.init(base38Representation: qrCodeId)
        do{
            _setupPayload = try parser.populatePayload()
            _deviceId = deviceId;
            _ssid = ssid;
            _ssidPassword = ssidPassword
            _call = call;
            handleRendezVous(deviceId: deviceId, setupPayload: _setupPayload!, rawPayload: qrCodeId)
        } catch {
            call.reject("-5")
        }
    }
    
    @objc public func readAttribute(deviceId: UInt64, endpointId: Int, clusterId: Int, attributeId: Int, call: CAPPluginCall )  -> Void {
        _deviceId = deviceId;
        _call = call;
        MTRGetConnectedDeviceWithID(deviceId){ chipDevice,error in
            if(error != nil){
                call.reject("-6")
                return;
            }
            let serialQueue = DispatchQueue(label: "com.csa.matter.qrcodevc.callback")           
            let attribute = MTRAttributeRequestPath.init(endpointID: endpointId as NSNumber, clusterID: clusterId as NSNumber, attributeID: attributeId as NSNumber)
            
            chipDevice?.readAttributePaths([attribute], eventPaths: nil, params: nil, queue: serialQueue) {result,error in 
                for (_, values) in result![0] {
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
        restartMatterStack()
        do {
            let setupPayload = try MTRSetupPayload.init(onboardingPayload: payload)
            try _controller?.setupCommissioningSession(with: setupPayload, newNodeID: deviceId as NSNumber)
        } catch {
            _call?.reject("-7")
            _call = nil
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
        Task {
//            await Task.sleep(3)
             _controller = InitializeMTR(_fabricId, _vendorId)
             // create commissioning params in ObjC. Pass those in here with network credentials.
             // maybe this just becomes the new norm
             let params = MTRCommissioningParameters.init();
             params.wifiSSID = _ssid.data(using: .utf8)
             params.wifiCredentials = _ssidPassword.data(using: .utf8)
             params.failSafeExpiryTimeoutSecs = 600;
             params.deviceAttestationDelegate = MatterAttestationDelegate.init()
             do {
                 try _controller?.commissionDevice(_deviceId, commissioningParams: params)
             } catch {
                 print("Failed to commission Device" + String(_deviceId) + ", with error ")
                 _call?.reject("-4")
                 _call = nil
             }
        }
        
       
    
    }
    
}

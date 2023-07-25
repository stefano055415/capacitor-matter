import Foundation
import Capacitor

@available(iOS 16.4, *)
public class CHIPToolDeviceAttestationDelegate {
    let _viewController: MTRDeviceControllerDelegate;
    
    init(viewController: MTRDeviceControllerDelegate) {
        self._viewController = viewController
    }
    
//    public func initWithViewController(viewController: Matter) -> CHIPToolDeviceAttestationDelegate
//    {
////        if (self = [super init]) {
////            _viewController = viewController;
////        }
//        return self;
//    }
}




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
//                    DispatchQueue.main.async {
                        self.commissionWithSSID()
//                    }
//                    dispatch_async(dispatch_get_main_queue(), ^{
//                        [self->_deviceList refreshDeviceList];
//                        [self retrieveAndSendWiFiCredentials];
//                        commissionWithSSID()
//                    });
                } else {
                    let params: MTRCommissioningParameters = MTRCommissioningParameters.init();
//                    params.deviceAttestationDelegate = CHIPToolDeviceAttestationDelegate.init(viewController: self);

                    params.failSafeExpiryTimeoutSecs = 600;

                    try _controller?.commissionDevice(_deviceId, commissioningParams: params)

//                    if (![controller commissionDevice:deviceId commissioningParams:params error:&error]) {
//                        NSLog(@"Failed to commission Device %llu, with error %@", deviceId, error);
//                    }
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
        _controller
        
    }
    
    
    private func handleRendezVous(deviceId: UInt64, setupPayload: MTRSetupPayload, rawPayload: String)
    {
        if (setupPayload.rendezvousInformation == nil) {
//            NSLog(@"Rendezvous Default");
            handleRendezVousDefault(deviceId: deviceId, payload: rawPayload)
//            [self handleRendezVousDefault:rawPayload];
            return;
        }
        let capabilities = setupPayload.discoveryCapabilities
        switch(setupPayload.discoveryCapabilities){
            
//        case MTRDiscoveryCapabilities.onNetwork,
//         MTRDiscoveryCapabilities.BLE,
//         MTRDiscoveryCapabilities.allMask:
//            print("Rendezvous Default");
//            break;
        case MTRDiscoveryCapabilities.softAP:
            print("Rendezvous Wi-Fi");
            break;
        default:
            print("Rendezvous Default");
            handleRendezVousDefault(deviceId: deviceId, payload: rawPayload)
        }

//        // TODO: This is a pretty broken way to handle a bitmask.
//        switch ([payload.rendezvousInformation unsignedLongValue]) {
//        case MTRDiscoveryCapabilitiesNone:
//        case MTRDiscoveryCapabilitiesOnNetwork:
//        case MTRDiscoveryCapabilitiesBLE:
//        case MTRDiscoveryCapabilitiesAllMask:
//            NSLog(@"Rendezvous Default");
//            [self handleRendezVousDefault:rawPayload];
//            break;
//        case MTRDiscoveryCapabilitiesSoftAP:
//            NSLog(@"Rendezvous Wi-Fi");
//            [self handleRendezVousWiFi:[self getNetworkName:payload.discriminator]];
//            break;
//        }
    }
    
    private func handleRendezVousDefault(deviceId: UInt64, payload: String)
    {
        let deviceID = MTRGetNextAvailableDeviceID();

        restartMatterStack()
        do{
            let result = try _controller!.pairDevice(deviceId, onboardingPayload: payload)
//            if (result) {
//                deviceID++;
//                MTRSetNextAvailableDeviceID(deviceID);
//            }
        } catch{
            
        }
        
    }
    
    
    private func restartMatterStack ()
    {
        _controller = MTRRestartController(_controller!);
        let concurrentQueue = DispatchQueue(label: "com.csa.matter.qrcodevc.callback")
        _controller!.setDeviceControllerDelegate(self, queue: concurrentQueue)
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
     //        params.deviceAttestationDelegate = [[CHIPToolDeviceAttestationDelegate alloc] initWithViewController:self];
             params.failSafeExpiryTimeoutSecs = 600;

     //        uint64_t deviceId = MTRGetNextAvailableDeviceID() - 1;
             do{
                 try _controller?.commissionDevice(_deviceId, commissioningParams: params)
             }catch{
                 print("Failed to commission Device" + String(_deviceId) + ", with error ")
             }
        }
        
       
    
    }
    
}

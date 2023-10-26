var capacitorMatter = (function (exports, core) {
    'use strict';

    exports.ErrorStatus = void 0;
    (function (ErrorStatus) {
        ErrorStatus["Close"] = "-1";
        ErrorStatus["GenericError"] = "-2";
        ErrorStatus["RendezVousError"] = "-3";
        ErrorStatus["StartCommissioningError"] = "-4";
        ErrorStatus["ParseQrCodeError"] = "-5";
        ErrorStatus["GetDeviceConnectedError"] = "-6";
        ErrorStatus["PairDeviceError"] = "-7";
        ErrorStatus["CommissioningEndError"] = "-8";
        ErrorStatus["BluetoothDisabled"] = "-9";
        ErrorStatus["ParseManualCodeError"] = "-10";
        ErrorStatus["getDeviceError"] = "-11";
        ErrorStatus["openCommissioningWindowError"] = "-12";
    })(exports.ErrorStatus || (exports.ErrorStatus = {}));

    const Matter = core.registerPlugin('Matter', {
        web: () => Promise.resolve().then(function () { return web; }).then(m => new m.MatterWeb()),
    });

    class MatterWeb extends core.WebPlugin {
        removeFabric(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        removeAllFabric(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        openCommissioningWindow(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        qrCodeCommissioning(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        manualCodeCommissioning(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        getCerts() {
            throw new Error('Method not implemented.');
        }
        readAttribute(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        subscribeAttribute(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        configure(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        clear() {
            throw new Error('Method not implemented.');
        }
        commandOnOff(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        getEndpoint(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        getCluster(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
        getAttribute(options) {
            console.log(options);
            throw new Error('Method not implemented.');
        }
    }

    var web = /*#__PURE__*/Object.freeze({
        __proto__: null,
        MatterWeb: MatterWeb
    });

    exports.Matter = Matter;

    Object.defineProperty(exports, '__esModule', { value: true });

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map

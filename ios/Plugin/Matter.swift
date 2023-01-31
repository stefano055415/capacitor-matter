import Foundation

@objc public class Matter: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}

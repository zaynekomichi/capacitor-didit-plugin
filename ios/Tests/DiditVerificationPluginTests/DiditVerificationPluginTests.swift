import XCTest
@testable import DiditVerificationPlugin

class DiditVerificationPluginTests: XCTestCase {
    func testPluginIdentity() {
        let plugin = DiditVerificationPlugin()
        XCTAssertEqual(plugin.jsName, "DiditVerification")
        XCTAssertEqual(plugin.pluginMethods.count, 1)
    }
}

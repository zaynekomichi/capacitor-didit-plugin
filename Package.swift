// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorDiditPlugin",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorDiditPlugin",
            targets: ["DiditVerificationPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0"),
        .package(url: "https://github.com/didit-protocol/sdk-ios.git", from: "4.1.0")
    ],
    targets: [
        .target(
            name: "DiditVerificationPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "DiditSDK", package: "sdk-ios")
            ],
            path: "ios/Sources/DiditVerificationPlugin"),
        .testTarget(
            name: "DiditVerificationPluginTests",
            dependencies: ["DiditVerificationPlugin"],
            path: "ios/Tests/DiditVerificationPluginTests")
    ]
)
